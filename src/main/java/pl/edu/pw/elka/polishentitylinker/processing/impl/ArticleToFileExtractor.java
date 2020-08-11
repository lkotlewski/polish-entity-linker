package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.ArticleToFileExtractorConfig;
import pl.edu.pw.elka.polishentitylinker.processing.exception.ImportException;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleToFileExtractor extends LineFileProcessor {

    private static final String FILENAME_PATTERN = "%d.tsv";

    private final ArticleToFileExtractorConfig config;

    private final StringBuilder stringBuilder = new StringBuilder();

    private Integer lastDocId;

    @Override
    public void processFile() {
        processLineByLine(config.getCorpusFilepath());
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
                Files.write(Paths.get(config.getOutDirectoryFilepath(), String.format(FILENAME_PATTERN, lastDocId)),
                        stringBuilder.toString().getBytes());
            } catch (IOException e) {
                throw new ImportException(e.getMessage());
            }
        }
    }
}
