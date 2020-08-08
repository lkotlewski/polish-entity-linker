package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.integration.wikidata.WikiDataSparqlClient;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.CategoriesProcessorConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoriesProcessor {

    private final CategoriesProcessorConfig config;
    private final BatchProcessingConfig batchProcessingConfig;
    private final WikiDataSparqlClient wikiDataSparqlClient;
    private final WikiItemRepository wikiItemRepository;

    @Value("${categories.files.directory}")
    private String directoryOut;

    public void processCategoriesStructure() {
        config.getRootCategories().forEach(category -> {
            wikiDataSparqlClient.downloadInstancesOf(category);
            wikiDataSparqlClient.downloadSubclassesOf(category);

            processConnectedToCategory(category);
        });
        log.info("Process finished");
    }

    private void processConnectedToCategory(String category) {
        Optional<WikiItemEntity> rootCategoryResult = wikiItemRepository.findById(category);
        rootCategoryResult.ifPresent(rootCategory -> {
            log.info("Category: {}", rootCategory.getId());
            processDirectlyConnected(rootCategory);
            processIndirectlyCategoryConnected(rootCategory);
        });
    }

    private void processDirectlyConnected(WikiItemEntity rootCategory) {
        String rootCategoryId = rootCategory.getId();
        Integer instancesCount = wikiItemRepository.countAllByInstanceOfIsContaining(rootCategoryId);
        Integer subclassesCount = wikiItemRepository.countAllBySubclassOfIsContaining(rootCategoryId);
        log.info("Instances count {}", instancesCount);
        log.info("Subclasses count {}", subclassesCount);

        int pageSize = 1000;
        int i;
        for(i = 0; i * pageSize < instancesCount; i++) {
            log.info("Instances page no: {}", i);
            PageRequest pageRequest = PageRequest.of(i, pageSize);
            List<WikiItemEntity> instances = wikiItemRepository.findByInstanceOfIsContaining(rootCategoryId, pageRequest);
            instances.forEach(instance -> instance.setRootCategory(rootCategory));
            wikiItemRepository.saveAll(instances);
        }
        for(i = 0; i * pageSize < subclassesCount; i++) {
            log.info("Subclasses page no: {}", i);
            PageRequest pageRequest = PageRequest.of(i, pageSize);
            List<WikiItemEntity> subclasses = wikiItemRepository.findBySubclassOfIsContaining(rootCategoryId, pageRequest);
            subclasses.forEach(subclass -> subclass.setRootCategory(rootCategory));
            wikiItemRepository.saveAll(subclasses);
        }
    }

    private void processIndirectlyCategoryConnected(WikiItemEntity rootCategory) {
        Path instancesPath = Paths.get(directoryOut, String.format(WikiDataSparqlClient.INSTANCES_FILENAME_PATTERN, rootCategory.getId()));
        Path subclassesPath = Paths.get(directoryOut, String.format(WikiDataSparqlClient.SUBCLASSES_FILENAME_PATTERN, rootCategory.getId()));
        processCategoryConnectedPath(instancesPath, rootCategory);
        processCategoryConnectedPath(subclassesPath, rootCategory);
    }

    private void processCategoryConnectedPath(Path path, WikiItemEntity rootCategory) {
        CategoryConnectedProcessor categoryConnectedProcessor = new CategoryConnectedProcessor(
                path, batchProcessingConfig, wikiItemRepository, rootCategory);
        categoryConnectedProcessor.processFile();
    }
}
