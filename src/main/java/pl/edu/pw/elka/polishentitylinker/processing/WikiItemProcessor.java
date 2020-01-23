package pl.edu.pw.elka.polishentitylinker.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;
import pl.edu.pw.elka.polishentitylinker.service.WikiItemService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiItemProcessor extends LineFileProcessor {

    private final WikiItemService wikiItemService;
    private final ItemsParserConfig itemsParserConfig;

    @Override
    public void parseFile() {
        parseFile(itemsParserConfig.getWikiItemsFilepath());
    }

    @Override
    void processLine(String line) {
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
}
