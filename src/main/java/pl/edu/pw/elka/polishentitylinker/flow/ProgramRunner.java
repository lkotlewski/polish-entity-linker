package pl.edu.pw.elka.polishentitylinker.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.EntityLinker;
import pl.edu.pw.elka.polishentitylinker.processing.impl.*;

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
    private final CorpusSanitizer corpusSanitizer;
    private final CorpusProcessor corpusProcessor;
    private final ArticleToFileExtractor articleToFileExtractor;
    private final TestTrainSetsPreparator testTrainSetsPreparator;


    private Map<ProgramOption, Runnable> actions;

    @Value("${program.option}")
    private String programOption;

    @PostConstruct
    private void fillActions() {
        actions = new HashMap<>();
        actions.put(ProgramOption.IMPORT_WIKI_ITEMS, wikiItemParser::processFile);
        actions.put(ProgramOption.IMPORT_PAGES, pageParser::processFile);
        actions.put(ProgramOption.IMPORT_REDIRECTS, redirectPageProcessor::processFile);
        actions.put(ProgramOption.SANITIZE_CORPUS, corpusSanitizer::processFile);
        actions.put(ProgramOption.PROCESS_CORPUS, corpusProcessor::processFile);
        actions.put(ProgramOption.EXTRACT_ARTICLES, articleToFileExtractor::processFile);
        actions.put(ProgramOption.PREPARE_TRAIN_TEST, testTrainSetsPreparator::processFile);
        actions.put(ProgramOption.LINK_ENTITIES, entityLinker::linkEntities);
    }

    public void run() {
        ProgramOption programOption = ProgramOption.valueOf(this.programOption);
        actions.get(programOption).run();
    }


}
