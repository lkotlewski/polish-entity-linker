package pl.edu.pw.elka.polishentitylinker.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.EntityLinker;
import pl.edu.pw.elka.polishentitylinker.processing.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgramRunner {

    private final WikiItemProcessor wikiItemParser;
    private final PageProcessor pageParser;
    private final RedirectPageProcessor redirectPageProcessor;
    private final EntityLinker entityLinker;
    private final TokensWithEntitiesProcessor tokensWithEntitiesProcessor;
    private final ArticlesProcessor articlesProcessor;
    private final TestTrainSetsPreparator testTrainSetsPreparator;

    private Map<ProgramOption, Runnable> actions;

    @Value("${program.option}")
    private String programOption;

    @PostConstruct
    private void fillActions() {
        actions = new HashMap<>();
        actions.put(ProgramOption.IMPORT_WIKI_ITEMS, wikiItemParser::parseFile);
        actions.put(ProgramOption.IMPORT_PAGES, pageParser::parseFile);
        actions.put(ProgramOption.IMPORT_REDIRECTS, redirectPageProcessor::parseFile);
        actions.put(ProgramOption.LINK_ENTITIES, entityLinker::linkEntities);
        actions.put(ProgramOption.COUNT_MENTIONS, tokensWithEntitiesProcessor::parseFile);
        actions.put(ProgramOption.EVAL_ARTICLES_LENGTH, articlesProcessor::parseFile);
        actions.put(ProgramOption.PREPARE_TRAIN_TEST, testTrainSetsPreparator::parseFile);
    }

    public void run() {
        ProgramOption programOption = ProgramOption.valueOf(this.programOption);
        actions.get(programOption).run();
    }


}
