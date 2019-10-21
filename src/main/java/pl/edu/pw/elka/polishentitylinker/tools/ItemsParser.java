package pl.edu.pw.elka.polishentitylinker.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;
import pl.edu.pw.elka.polishentitylinker.service.WikiItemService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@AllArgsConstructor
public class ItemsParser {

    private WikiItemService wikiItemService;

    public void parseFile(String path){
        Path path1 = Paths.get(path);

        try {
            Files.lines(path1).forEach(line -> {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    WikiItem wikiItem = objectMapper.readValue(line, WikiItem.class);
                    wikiItemService.add(wikiItem);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
