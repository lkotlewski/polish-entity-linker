package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.exception.ImportException;
import pl.edu.pw.elka.polishentitylinker.model.DividedArticle;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.ArticleDivider;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestTrainSetsPreparator extends LineFileProcessor {

    private final String FILENAME_PATTERN = "%d.tsv";

    private final WikiItemRepository wikiItemRepository;
    private final ItemsParserConfig itemsParserConfig;

    private StringBuilder stringBuilder = new StringBuilder();

    private List<Integer> longestArticlesPageIds;
    private Integer lastDocId;

    @Override
    public void processFile() {
        List<WikiItemEntity> longestArticles = wikiItemRepository.findLongestArticles(10000);
        longestArticlesPageIds = longestArticles.stream()
                .map(WikiItemEntity::getPageId)
                .collect(Collectors.toList());
        processLineByLine(itemsParserConfig.getTrainDirectoryFilepath());
        divideLongestArticles();
    }

    @Override
    protected void processLine(String line) {
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null) {
            Integer docId = tokenizedWord.getDocId();
            if (!docId.equals(lastDocId)) {
                savePreviousArticleToFile();
                stringBuilder.setLength(0);
                log.info("processing {}", docId);
                lastDocId = docId;
            }
        }
        stringBuilder.append(line).append("\n");
    }

    private void savePreviousArticleToFile() {
        if (stringBuilder.length() != 0) {
            try {
                Files.write(Paths.get(itemsParserConfig.getTrainDirectoryFilepath(), String.format(FILENAME_PATTERN, lastDocId)),
                        stringBuilder.toString().getBytes());
            } catch (IOException e) {
                throw new ImportException(e.getMessage());
            }
        }
    }

    private void divideLongestArticles() {
        Path testFilepath = Paths.get(itemsParserConfig.getTestFilepath());
        longestArticlesPageIds.forEach(docId -> {
                    String filename = String.format(FILENAME_PATTERN, docId);
                    Path filePath =
                            Paths.get(itemsParserConfig.getTrainDirectoryFilepath(), filename);
                    try {
                        DividedArticle dividedArticle = ArticleDivider.divideArticle(filePath);
                        Files.write(testFilepath, dividedArticle.getTestPart(), StandardOpenOption.APPEND);
                        Files.copy(filePath, Paths.get(itemsParserConfig.getBackupFolderFilepath(), filename));
                        Files.delete(filePath);
                        Files.write(filePath, dividedArticle.getTrainPart());
                    } catch (IOException e) {
                        throw new ImportException(e.getMessage());
                    }
                    log.info("{} divided", docId);
                }
        );
    }
}
