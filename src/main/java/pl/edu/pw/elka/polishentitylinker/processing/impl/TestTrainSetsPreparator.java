package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.exception.ImportException;
import pl.edu.pw.elka.polishentitylinker.model.DividedArticle;
import pl.edu.pw.elka.polishentitylinker.processing.FileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.TestTrainPreparatorConfig;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.ArticleDivider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestTrainSetsPreparator implements FileProcessor {

    private final String FILENAME_PATTERN = "%d.tsv";

    private final WikiItemRepository wikiItemRepository;
    private final TestTrainPreparatorConfig config;

    private List<Integer> longestArticlesPageIds;

    @Override
    public void processFile() {
        List<WikiItemEntity> longestArticles = wikiItemRepository.findLongestArticles(config.getNumberToDivide());
        longestArticlesPageIds = longestArticles.stream()
                .map(WikiItemEntity::getPageId)
                .collect(Collectors.toList());
        divideLongestArticles();
    }

    private void divideLongestArticles() {
        Path testFilepath = Paths.get(config.getTestFilepath());
        longestArticlesPageIds.forEach(docId -> {
                    String filename = String.format(FILENAME_PATTERN, docId);
                    Path trainFilepath =
                            Paths.get(config.getTrainDirectory(), filename);
                    try {
                        DividedArticle dividedArticle = ArticleDivider.divideArticle(trainFilepath, config.getTrainLeftPart());
                        Files.write(testFilepath, dividedArticle.getTestPart(), StandardOpenOption.APPEND);
                        Files.copy(trainFilepath, Paths.get(config.getBackupDirectory(), filename));
                        Files.delete(trainFilepath);
                        Files.write(trainFilepath, dividedArticle.getTrainPart());
                    } catch (IOException e) {
                        throw new ImportException(e.getMessage());
                    }
                    log.info("{} divided", docId);
                }
        );
    }
}
