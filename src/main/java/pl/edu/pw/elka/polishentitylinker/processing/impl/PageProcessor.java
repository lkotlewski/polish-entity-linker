package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.Page;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.processing.BufferedBatchProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final BatchProcessingConfig batchProcessingConfig;
    private final WikiItemRepository wikiItemRepository;

    private BufferedBatchProcessor<WikiItemEntity> wikiItemSaver;

    @Override
    public void processFile() {
        wikiItemSaver = new BufferedBatchProcessor<>(
                wikiItemRepository::saveAll, batchProcessingConfig.getSize());
        processLineByLine(itemsParserConfig.getPagesFilepath());
        wikiItemSaver.processRest();
    }

    @Override
    protected void processLine(String line) {
        Page page = new Page(line);
        log.info(String.valueOf(page.getPageId()));

        if (PageType.REGULAR_ARTICLE.equals(page.getType())) {
            WikiItemEntity wikiItemEntity =
                    wikiItemRepository.findByTitlePl(page.getTitle());
            if (wikiItemEntity != null) {
                wikiItemEntity.setPageId(page.getPageId());
                wikiItemSaver.process(wikiItemEntity);
            }
        }
    }
}

