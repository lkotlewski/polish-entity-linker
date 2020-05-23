package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.core.TaggedTextIterator;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.config.TuningDatasetPreparatorConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;
import pl.edu.pw.elka.polishentitylinker.utils.TokenizedTextUtils;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningDatasetPreparator {

    private static final String LINE_PATTERN = "%s\t%d\t%s\t%d\t%s\t%s\t%b\n";

    private final TuningDatasetPreparatorConfig config;
    private final WikiItemRepository wikiItemRepository;
    private final Searcher searcher;

    private final TaggedTextIterator taggedTextIterator = new TaggedTextIterator();

    private int examplesCount = 0;
    private Path outFilepath;

    private Map<String, Integer> positiveExamplesDistribution = new HashMap<>();
    private Map<String, Integer> negativeExamplesDistribution = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        outFilepath = Paths.get(config.getOutFilepath());
    }


    public void prepareDataset() {
        taggedTextIterator.processFile(config.getInFilepath());

        int i = 0;
        boolean allLengthEnoughArticlesProceed = false;

        while (examplesCount < config.getTrainExamplesCount() || allLengthEnoughArticlesProceed) {
            int pageSize = 500;
            List<WikiItemEntity> contextWikiItems = wikiItemRepository.findWithHigherArticleLength(
                    config.getTrainArticleMinLength(), PageRequest.of(i, pageSize));
            prepareExamplesPart(contextWikiItems);
            allLengthEnoughArticlesProceed = contextWikiItems.isEmpty();
            i++;
        }
        log.info("Process finished");
        logDistributionsStats();
    }

    private void prepareExamplesPart(List<WikiItemEntity> contextWikiItems) {
        int i = 0;
        while (i < contextWikiItems.size() && examplesCount < config.getTrainExamplesCount()) {
            prepareExamplesFrom(contextWikiItems.get(i));
            i++;
        }
    }

    private void prepareExamplesFrom(WikiItemEntity contextWikiItem) {
        taggedTextIterator.processFile(getArticlePath(contextWikiItem.getPageId()));
        NamedEntity targetNamedEntity = getTargetNamedEntity();

        if (targetNamedEntity != null) {
            Optional<WikiItemEntity> targetWikiItemById = wikiItemRepository.findById(targetNamedEntity.getEntityId());
            if (targetWikiItemById.isPresent() && targetWikiItemById.get().getRootCategory() != null) {
                WikiItemEntity targetWikiItem = targetWikiItemById.get();
                List<WikiItemEntity> falsePositiveCandidates = getFalsePositiveCandidates(targetNamedEntity);

                if (!falsePositiveCandidates.isEmpty()) {
                    WikiItemEntity falsePositiveCandidate = falsePositiveCandidates.get(0);
                    writeExampleToFile(outFilepath, prepareTruePositiveExample(contextWikiItem, targetNamedEntity, targetWikiItem));
                    writeExampleToFile(outFilepath, prepareFalsePositiveExample(contextWikiItem, targetNamedEntity, falsePositiveCandidate));
                    examplesCount += 2;
                    updateDistributionsStats(targetWikiItem, falsePositiveCandidate);
                    log.info("Examples count: {}/{}", examplesCount, config.getTrainExamplesCount());
                }
            }

        }
    }

    private void updateDistributionsStats(WikiItemEntity targetWikiItem, WikiItemEntity falsePositiveCandidate) {
        String positiveRootCategoryId = targetWikiItem.getRootCategory().getId();
        String negativeRootCategoryId = falsePositiveCandidate.getRootCategory() == null ? "Empty" :
                targetWikiItem.getRootCategory().getId();
        positiveExamplesDistribution.put(positiveRootCategoryId,
                getIncrementedExamplesFromCategoryCount(positiveExamplesDistribution, positiveRootCategoryId));
        negativeExamplesDistribution.put(positiveRootCategoryId,
                getIncrementedExamplesFromCategoryCount(negativeExamplesDistribution, negativeRootCategoryId));
    }

    private int getIncrementedExamplesFromCategoryCount(Map<String, Integer> distribution, String rootCategoryId) {
        return distribution.get(rootCategoryId) == null ? 1 : positiveExamplesDistribution.get(rootCategoryId) + 1;
    }

    private String prepareTruePositiveExample(WikiItemEntity contextWikiItem, NamedEntity targetNamedEntity, WikiItemEntity targetWikiItem) {
        return prepareDatasetLine(
                targetNamedEntity.toOriginalForm(),
                contextWikiItem.getPageId(),
                targetNamedEntity.getEntityId(),
                targetWikiItem.getPageId(),
                targetNamedEntity.getContextAsString(),
                getArticleBeginningByPageId(targetWikiItem.getPageId()),
                true
        );
    }


    private String prepareFalsePositiveExample(WikiItemEntity contextWikiItem, NamedEntity targetNamedEntity, WikiItemEntity falsePositiveCandidate) {
        return prepareDatasetLine(
                targetNamedEntity.toOriginalForm(),
                contextWikiItem.getPageId(),
                targetNamedEntity.getEntityId(),
                falsePositiveCandidate.getPageId(),
                targetNamedEntity.getContextAsString(),
                getArticleBeginningByPageId(falsePositiveCandidate.getPageId()),
                false
        );
    }

    private String prepareDatasetLine(String originalForm, Integer contextArticleId, String targetWikiItemId, Integer comparedArticleId,
                                      String context, String compared, boolean positiveExample) {
        return String.format(LINE_PATTERN, originalForm, contextArticleId, targetWikiItemId, comparedArticleId,
                context, compared, positiveExample);
    }

    private List<WikiItemEntity> getFalsePositiveCandidates(NamedEntity namedEntity) {
        return searcher.findCandidates(namedEntity)
                .stream()
                .filter(wikiItemEntity -> !wikiItemEntity.getId().equals(namedEntity.getEntityId()))
                .collect(Collectors.toList());
    }

    private NamedEntity getTargetNamedEntity() {
        List<NamedEntity> namedEntities = taggedTextIterator.getNamedEntities();
        if (namedEntities.size() > 0) {
            int middleIdx = namedEntities.size() / 2;
            return namedEntities.get(middleIdx);
        }
        return null;
    }

    private String getArticleBeginningByPageId(Integer pageId) {
        Path path = getArticlePath(pageId);
        List<TokenizedWord> collect = new ArrayList<>();
        try {
            collect = Files.lines(path)
                    .map(TsvLineParser::parseTokenizedWord)
                    .filter(Objects::nonNull)
                    .limit(config.getArticlePartSize())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return TokenizedTextUtils.spanToOriginalForm(collect);
    }

    private Path getArticlePath(Integer pageId) {
        return Paths.get(config.getArticlesDirectory(), String.format("%d.tsv", pageId));
    }

    private void writeExampleToFile(Path outFilepath, String line) {
        try {
            Files.write(outFilepath, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void logDistributionsStats() {
        log.info("Positive examples stats:");
        positiveExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Negative examples stats:");
        negativeExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
    }
}
