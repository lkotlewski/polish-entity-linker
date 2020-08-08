package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.core.TaggedTextIterator;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.processing.config.TuningDatasetPreparatorConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.searcher.Searcher;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static pl.edu.pw.elka.polishentitylinker.integration.gcp.BertIntegrationUtils.prepareExampleForClassifier;
import static pl.edu.pw.elka.polishentitylinker.utils.FileUtils.appendToFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningDatasetPreparator {

    private final TuningDatasetPreparatorConfig config;
    private final WikiItemRepository wikiItemRepository;
    private final Searcher searcher;

    private final TaggedTextIterator taggedTextIterator = new TaggedTextIterator();

    private int examplesCount = 0;
    private Path trainFilepath;
    private Path devFilepath;
    private Path testFilepath;

    private Path currentFilepath;


    private Map<String, Integer> trainPositiveExamplesDistribution = new HashMap<>();
    private Map<String, Integer> trainNegativeExamplesDistribution = new HashMap<>();
    private Map<String, Integer> devPositiveExamplesDistribution = new HashMap<>();
    private Map<String, Integer> devNegativeExamplesDistribution = new HashMap<>();
    private Map<String, Integer> testPositiveExamplesDistribution = new HashMap<>();
    private Map<String, Integer> testNegativeExamplesDistribution = new HashMap<>();

    private Map<String, Integer> currentPositiveExamplesDistribution;
    private Map<String, Integer> currentNegativeExamplesDistribution;

    private int sumExamplesCount;

    @PostConstruct
    private void postConstruct() {
        trainFilepath = Paths.get(config.getTrainFilepath());
        devFilepath = Paths.get(config.getDevFilepath());
        testFilepath = Paths.get(config.getTestFilepath());
        currentFilepath = trainFilepath;
        currentPositiveExamplesDistribution = trainPositiveExamplesDistribution;
        currentNegativeExamplesDistribution = trainNegativeExamplesDistribution;
        sumExamplesCount = getSumExamplesCount();
    }

    public void prepareDataset() {
        int i = 0;
        boolean allLongEnoughArticlesProceeded = false;

        while (examplesCount < sumExamplesCount || allLongEnoughArticlesProceeded) {
            int pageSize = 500;
            List<WikiItemEntity> contextWikiItems = wikiItemRepository.findWithHigherArticleLength(
                    config.getTrainArticleMinLength(), PageRequest.of(i, pageSize));
            prepareExamplesPart(contextWikiItems);
            allLongEnoughArticlesProceeded = contextWikiItems.isEmpty();
            i++;
        }
        log.info("Process finished");
        logDistributionsStats();
    }

    private void prepareExamplesPart(List<WikiItemEntity> contextWikiItems) {
        int i = 0;
        while (i < contextWikiItems.size() && examplesCount < sumExamplesCount) {
            prepareExamplesFrom(contextWikiItems.get(i));
            i++;
        }
    }

    private void prepareExamplesFrom(WikiItemEntity contextWikiItem) {
        taggedTextIterator.processFile(getArticlePath(contextWikiItem.getPageId()));

        List<NamedEntity> namedEntities = taggedTextIterator.getNamedEntities();
        int entitiesGap = 5;
        int i;
        for (i = 0; i < namedEntities.size(); i += entitiesGap) {
            NamedEntity targetNamedEntity = namedEntities.get(i);
            Optional<WikiItemEntity> targetWikiItemById = wikiItemRepository.findById(targetNamedEntity.getEntityId());
            if (targetWikiItemById.isPresent() && targetWikiItemById.get().getRootCategory() != null) {
                WikiItemEntity targetWikiItem = targetWikiItemById.get();
                List<WikiItemEntity> candidates = getCandidates(targetNamedEntity);
                candidates.forEach(candidate -> {
                    appendToFile(currentFilepath, prepareExampleForClassifier(contextWikiItem.getPageId(),
                            targetNamedEntity, candidate, config.getArticlePartSize(),
                            config.getArticlesDirectory()));
                    updateDistributionsStats(targetWikiItem, candidate);
                    examplesCount++;
                    log.info("Examples count: {}/{}", examplesCount, sumExamplesCount);
                    if (examplesCount == config.getTrainExamplesCount()) {
                        currentFilepath = devFilepath;
                        currentPositiveExamplesDistribution = devPositiveExamplesDistribution;
                        currentNegativeExamplesDistribution = devNegativeExamplesDistribution;
                        log.info("Creating dev set");
                    } else if (examplesCount == config.getTrainExamplesCount() + config.getDevExamplesCount()) {
                        currentFilepath = testFilepath;
                        currentPositiveExamplesDistribution = testPositiveExamplesDistribution;
                        currentNegativeExamplesDistribution = testNegativeExamplesDistribution;
                        log.info("Creating test set");
                    }
                });
            }
        }
    }

    private void updateDistributionsStats(WikiItemEntity targetWikiItem, WikiItemEntity candidateWikiItem) {
        if (targetWikiItem.getId().equals(candidateWikiItem.getId())) {
            String positiveRootCategoryId = targetWikiItem.getRootCategory().getId();
            currentPositiveExamplesDistribution.put(positiveRootCategoryId,
                    getIncrementedExamplesFromCategoryCount(currentPositiveExamplesDistribution, positiveRootCategoryId));
        } else {
            String negativeRootCategoryId = candidateWikiItem.getRootCategory() == null ? "Empty" :
                    targetWikiItem.getRootCategory().getId();
            currentNegativeExamplesDistribution.put(negativeRootCategoryId,
                    getIncrementedExamplesFromCategoryCount(currentNegativeExamplesDistribution, negativeRootCategoryId));
        }
    }

    private int getIncrementedExamplesFromCategoryCount(Map<String, Integer> distribution, String rootCategoryId) {
        return distribution.get(rootCategoryId) == null ? 1 : distribution.get(rootCategoryId) + 1;
    }

    private List<WikiItemEntity> getCandidates(NamedEntity namedEntity) {
        return searcher.findCandidates(namedEntity)
                .stream()
                .filter(wikiItemEntity -> wikiItemEntity.getRootCategory() != null)
                .collect(Collectors.toList());
    }

    private Path getArticlePath(Integer pageId) {
        return Paths.get(config.getArticlesDirectory(), String.format("%d.tsv", pageId));
    }

    private void logDistributionsStats() {
        log.info("Train positive examples stats:");
        trainPositiveExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Train negative examples stats:");
        trainNegativeExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Dev positive examples stats:");
        devPositiveExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Dev negative examples stats:");
        devNegativeExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Test positive examples stats:");
        testPositiveExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
        log.info("Test negative examples stats:");
        testNegativeExamplesDistribution.forEach((k, v) -> log.info("{} : {}", k, v));
    }

    private int getSumExamplesCount() {
        return config.getTrainExamplesCount() + config.getDevExamplesCount() + config.getTestExamplesCount();
    }
}
