package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.processing.config.DividedCorpusProcessorConfig;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DividedCorpusProcessor {

    private final DividedCorpusProcessorConfig config;

    private Path inFilepath;
    private Path outFilepath;

    @PostConstruct
    private void postConstruct() {
        inFilepath = Paths.get(config.getFilepath());
        outFilepath = Paths.get(config.getOutFilepath());
    }

    public void processFile() {
        try {
            AtomicInteger fileProcessedCount = new AtomicInteger(0);
            long allCount = Files.walk(Paths.get(config.getFilepath())).count();
            Files.walk(inFilepath).forEach(path -> {
                try {
                    Files.write(outFilepath, Files.readAllBytes(path), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                log.info("{}/{}", fileProcessedCount.incrementAndGet(), allCount);
            });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
