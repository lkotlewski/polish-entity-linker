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

    private final String FILENAME_PATTERN = "%d.tsv";
    private static final String REF_TOKEN = "ref";
    private static final String SLASH = "/";

    private final ArticleToFileExtractorConfig config;

    private final StringBuilder stringBuilder = new StringBuilder();

    private Integer lastDocId;
    private boolean processingRef = false;
    private boolean lastSlash = false;
    private boolean refEnd = false;

    @Override
    public void processFile() {
        processLineByLine(config.getCorpusFilepath());
    }

    @Override
    protected void processLine(String line) {
        refEnd = false;
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null) {
            Integer docId = tokenizedWord.getDocId();
            if (!docId.equals(lastDocId)) {
                savePreviousArticleToFile();
                stringBuilder.setLength(0);
                log.info("processing {}", docId);
                lastDocId = docId;
                processingRef = false;
            }

            if (REF_TOKEN.equals(tokenizedWord.getToken())) {
                if (lastSlash) {
                    refEnd = true;
                } else {
                    processingRef = true;
                }
                lastSlash = false;
            } else if (SLASH.equals(tokenizedWord.getToken())) {
                if (processingRef) {
                    processingRef = false;
                    lastSlash = true;
                    refEnd = true;
                }
            } else {
                lastSlash = false;
            }
        }
        if (!processingRef && !refEnd) {
            stringBuilder.append(line).append("\n");
        }
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
