package pl.edu.pw.elka.polishentitylinker.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;
import pl.edu.pw.elka.polishentitylinker.service.WikiItemService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiItemParser extends LineFileParser {

    private final WikiItemService wikiItemService;
    private final ItemsParserConfig itemsParserConfig;

    @Override
    public void parseFile() {
        parseFile(itemsParserConfig.getWikiItemsFilepath(), this::readEntitiesFileLine);
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
}
