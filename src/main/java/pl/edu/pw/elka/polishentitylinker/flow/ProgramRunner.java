package pl.edu.pw.elka.polishentitylinker.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.imports.PageParser;
import pl.edu.pw.elka.polishentitylinker.imports.WikiItemParser;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgramRunner {

    private final WikiItemParser wikiItemParser;
    private final PageParser pageParser;

    private Map<ProgramOption, Runnable> actions;

    @Value("${program.option}")
    private String programOption;

    @PostConstruct
    private void fillActions() {
        actions = new HashMap<>();
        actions.put(ProgramOption.IMPORT_WIKI_ITEMS, wikiItemParser::parseFile);
        actions.put(ProgramOption.IMPORT_PAGES, pageParser::parseFile);
    }

    public void run() {
        ProgramOption programOption = ProgramOption.valueOf(this.programOption);
        actions.get(programOption).run();
    }


}
