package pl.edu.pw.elka.polishentitylinker.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.RedirectPageEntity;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.imports.summary.PagesImportSummary;
import pl.edu.pw.elka.polishentitylinker.model.csv.Page;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;
import pl.edu.pw.elka.polishentitylinker.repository.RedirectPageRepository;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.WikiItemService;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemsParser {

    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemService wikiItemService;
    private final WikiItemRepository wikiItemRepository;
    private final RedirectPageRepository redirectPageRepository;
    private final PagesImportSummary pagesImportSummary = new PagesImportSummary();

    private BufferedBatchProcessor<WikiItemEntity> wikiItemSaver;
    private BufferedBatchProcessor<RedirectPageEntity> redirectPageSaver;

    public void parseEntitiesFile() {
        parseFile(itemsParserConfig.getWikiItemsFilepath(), this::readEntitiesFileLine);
    }

    public void parsePagesFile() {
        wikiItemSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        redirectPageSaver = new BufferedBatchProcessor<>(redirectPageRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        parseFile(itemsParserConfig.getPagesFilepath(), this::readPagesFileLine);
        pagesImportSummary.printSummary();
    }

    private void parseFile(String path, Consumer<String> action) {
        Path path1 = Paths.get(path);

        try {
            Files.lines(path1).forEach(action);
        } catch (IOException e) {
            log.error("", e);
        }
    }


    private void readEntitiesFileLine(String line) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            WikiItem wikiItem = objectMapper.readValue(line, WikiItem.class);
            if (wikiItem.getLabelPl() != null || wikiItem.getTitlePl() != null) {
                wikiItemService.add(wikiItem);
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private void readPagesFileLine(String line) {
        Page page = new Page(line);
        log.info(String.valueOf(page.getPageId()));

        pagesImportSummary.incrementCounts(page.getType());

        if (page.getType() != null && !PageType.TEMPLATE.equals(page.getType())) {
            if (PageType.REDIRECT.equals(page.getType())) {
                redirectPageSaver.addToBuffer(new RedirectPageEntity(page.getPageId(), page.getTitle()));
            } else {
                WikiItemEntity wikiItemEntity = wikiItemRepository.findByTitlePl(page.getTitle());
                if (wikiItemEntity != null) {
                    wikiItemEntity.setPageId(page.getPageId());
                    wikiItemEntity.setPageType(page.getType());
                    wikiItemSaver.addToBuffer(wikiItemEntity);
                }
            }
        }
    }

}
