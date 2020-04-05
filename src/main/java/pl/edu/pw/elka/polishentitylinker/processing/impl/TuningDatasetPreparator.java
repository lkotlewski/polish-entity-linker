package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningDatasetPreparator {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final TuningDatasetPreparatorConfig config;
    private final WikiItemRepository wikiItemRepository;
    private final Searcher searcher;

    private final TaggedTextIterator taggedTextIterator = new TaggedTextIterator();


    public void prepareDataset() {
        taggedTextIterator.processFile(config.getInFilepath());
        List<WikiItemEntity> contextWikiItems = wikiItemRepository.findWithHigherArticleLength(
                config.getTrainArticleMinLength(), config.getTrainArticlesCount());

        IntStream.range(0, 100).forEach(
                i -> {
                    WikiItemEntity contextWikiItem = contextWikiItems.get(i);
                    taggedTextIterator.processFile(getArticlePath(contextWikiItem.getPageId()));
                    NamedEntity targetNamedEntity = getTargetNamedEntity();

                    if (targetNamedEntity != null) {
                        Optional<WikiItemEntity> targetWikiItemById = wikiItemRepository.findById(targetNamedEntity.getEntityId());
                        if (targetWikiItemById.isPresent()) {
                            WikiItemEntity targetWikiItem = targetWikiItemById.get();
                            List<WikiItemEntity> candidates = getFalsePositiveCandidates(targetNamedEntity);

                            if (candidates.size() > 1) {
                                WikiItemEntity falsePositiveCandidate = candidates.get(0);


                                System.out.println(prepareDatasetLine(
                                        targetNamedEntity.toOriginalForm(),
                                        contextWikiItem.getPageId(),
                                        targetNamedEntity.getEntityId(),
                                        targetWikiItem.getPageId(),
                                        targetNamedEntity.getContextAsString(),
                                        getArticleBeginningByPageId(targetWikiItem.getPageId()),
                                        true
                                ));

                                System.out.println(prepareDatasetLine(
                                        targetNamedEntity.toOriginalForm(),
                                        contextWikiItem.getPageId(),
                                        targetNamedEntity.getEntityId(),
                                        falsePositiveCandidate.getPageId(),
                                        targetNamedEntity.getContextAsString(),
                                        getArticleBeginningByPageId(falsePositiveCandidate.getPageId()),
                                        false
                                ));

                            }
                            System.out.println("**********************************************************************************************");
                        }

                    }
                }
        );
    }

    private String prepareDatasetLine(String originalForm, Integer contextArticleId, String targetWikiItemId, Integer comparedArticleId,
                                      String context, String compared, boolean positiveExample) {
        return String.format("%s\t%d\t%s\t%d\t%s\t%s\t%b", originalForm, contextArticleId, targetWikiItemId, comparedArticleId,
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
            NamedEntity namedEntity = namedEntities.get(middleIdx);
            String originalForm = namedEntity.toOriginalForm();
            Matcher matcher = NUMBER_PATTERN.matcher(originalForm);

            if (!matcher.matches()) {
                return namedEntity;
            }
        }
        return null;
    }

    private String getArticleBeginningById(String id) {
        Optional<WikiItemEntity> byId = wikiItemRepository.findById(id);
        if (byId.isPresent()) {
            WikiItemEntity wikiItemEntity = byId.get();
            if (wikiItemEntity.getPageId() != null) {
                return getArticleBeginningByPageId(wikiItemEntity.getPageId());
            }
        }
        return "";
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
}
