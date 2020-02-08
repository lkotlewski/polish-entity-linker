package pl.edu.pw.elka.polishentitylinker.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.EntityLinker;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.processing.impl.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgramRunner {

    private final ItemsParserConfig config;
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
        actions.put(ProgramOption.IMPORT_WIKI_ITEMS, () -> wikiItemParser.processFile(config.getWikiItemsFilepath()));
        actions.put(ProgramOption.IMPORT_PAGES, () -> pageParser.processFile(config.getPagesFilepath()));
        actions.put(ProgramOption.IMPORT_REDIRECTS, () -> redirectPageProcessor.processFile(config.getRedirectFilepath()));
        actions.put(ProgramOption.COUNT_MENTIONS, () -> tokensWithEntitiesProcessor.processFile(config.getTokensWithEntitiesFilepath()));
        actions.put(ProgramOption.EVAL_ARTICLES_LENGTH, () -> articlesProcessor.processFile(config.getTokensWithEntitiesFilepath()));
        actions.put(ProgramOption.PREPARE_TRAIN_TEST, () -> testTrainSetsPreparator.processFile(config.getTokensWithEntitiesFilepath()));
        actions.put(ProgramOption.LINK_ENTITIES, entityLinker::linkEntities);
    }

    public void run() {
        ProgramOption programOption = ProgramOption.valueOf(this.programOption);
        actions.get(programOption).run();
    }


}
