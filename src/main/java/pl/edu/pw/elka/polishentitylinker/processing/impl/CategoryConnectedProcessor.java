package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CategoryConnectedProcessor extends LineFileProcessor {

    private final Path filePath;
    private final BatchProcessingConfig batchProcessingConfig;
    private final WikiItemRepository wikiItemRepository;
    private final WikiItemEntity rootCategory;

    private BufferedBatchProcessor<WikiItemEntity> wikiItemSaver;

    @Override
    public void processFile() {
        wikiItemSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, batchProcessingConfig.getSize());
        processLineByLine(filePath.toAbsolutePath().toString());
        wikiItemSaver.processRest();
    }

    @Override
    protected void processLine(String line) {
        Optional<WikiItemEntity> wikiItemResult = wikiItemRepository.findById(line);
        wikiItemResult.ifPresent(wikiItemEntity -> {
            if(wikiItemEntity.getRootCategory() == null) {
                wikiItemEntity.setRootCategory(rootCategory);
                wikiItemSaver.process(wikiItemEntity);
            }
        });
    }
}
