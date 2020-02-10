package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.CorpusSanitizerConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorpusSanitizer extends LineFileProcessor {

    private static final String REF_TOKEN = "ref";

    private final ItemsParserConfig itemsParserConfig;
    private final CorpusSanitizerConfig config;

    private BufferedBatchProcessor<String> fileAppender;
    private Path outFilepath;
    private boolean processingRef = false;

    @Override
    public void processFile() {
        fileAppender = new BufferedBatchProcessor<>(this::appendLines, itemsParserConfig.getSaveBatchSize());
        outFilepath = Paths.get(config.getOutFilepath());
        processLineByLine(config.getInFilepath());
        fileAppender.processRest();
    }


    @Override
    protected void processLine(String line) {
        boolean refBorder = false;
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null && REF_TOKEN.equals(tokenizedWord.getToken())) {
            processingRef = !processingRef;
            refBorder = true;
        }
        if (!processingRef && !refBorder) {
            fileAppender.process(line);
        }
    }

    private void appendLines(List<String> lines) {
        String textPart = String.join("\n", lines);
        try {
            Files.write(outFilepath, textPart.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


}

