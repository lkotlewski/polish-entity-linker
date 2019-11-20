package pl.edu.pw.elka.polishentitylinker.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.imports.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.imports.summary.PagesImportSummary;
import pl.edu.pw.elka.polishentitylinker.model.csv.Page;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemRepository wikiItemRepository;
    private final PagesImportSummary pagesImportSummary = new PagesImportSummary();

    private BufferedBatchProcessor<WikiItemEntity> wikiItemSaver;

    @Override
    public void parseFile() {
        wikiItemSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        parseFile(itemsParserConfig.getPagesFilepath());
        wikiItemSaver.processRest();
        pagesImportSummary.printSummary();
    }

    @Override
    void processLine(String line) {
        Page page = new Page(line);
        log.info(String.valueOf(page.getPageId()));

        pagesImportSummary.incrementCounts(page.getType());

        if (page.getType() != null && !PageType.TEMPLATE.equals(page.getType()) && !PageType.REDIRECT.equals(page.getType())) {
            WikiItemEntity wikiItemEntity = wikiItemRepository.findByTitlePl(page.getTitle());
            if (wikiItemEntity != null) {
                wikiItemEntity.setPageId(page.getPageId());
                wikiItemEntity.setPageType(page.getType());
                wikiItemSaver.process(wikiItemEntity);
            }

        }
    }
}

