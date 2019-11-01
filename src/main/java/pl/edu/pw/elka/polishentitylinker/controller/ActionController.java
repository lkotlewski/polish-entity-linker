package pl.edu.pw.elka.polishentitylinker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pl.edu.pw.elka.polishentitylinker.tools.ItemsParser;

@Controller
@RequiredArgsConstructor
public class ActionController {

    @Value("${datasource.wiki-items.filepath}")
    String wikiItemsFilepath;

    private final ItemsParser itemsParser;

    @GetMapping("import_database")
    public void importDatabase() {
        itemsParser.parseFile(wikiItemsFilepath);
    }
}
