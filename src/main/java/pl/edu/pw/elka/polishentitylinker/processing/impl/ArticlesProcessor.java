package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.FileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticlesProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemRepository wikiItemRepository;

    private BufferedBatchProcessor<WikiItemEntity> articlesLengthSaver;
    private Map<Integer, Integer> articlesLength = new HashMap<>();
    private Integer lastDocId;

    @Override
    public void processFile(String pathToFile) {
        articlesLengthSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        processLineByLine(pathToFile);
        saveResults();
    }

    @Override
    protected void processLine(String line) {
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null) {
            Integer docId = tokenizedWord.getDocId();
            if (articlesLength.containsKey(docId)) {
                articlesLength.put(docId, articlesLength.get(docId) + 1);
            } else {
                articlesLength.put(docId, 1);
            }
            if (!docId.equals(lastDocId)) {
                log.info("processing {}", docId);
                lastDocId = docId;
            }
        }
    }

    private void saveResults() {
        log.info("saving results");
        articlesLength.forEach((docId, articleLength) -> {
            log.info(docId.toString());
            wikiItemRepository.findByPageId(docId).ifPresent(wikiItemEntity -> {
                wikiItemEntity.setArticleLength(articleLength);
                articlesLengthSaver.process(wikiItemEntity);
            });
        });
        articlesLengthSaver.processRest();
    }
}
