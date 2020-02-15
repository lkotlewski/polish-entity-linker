package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.CorpusSanitizerConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.DirectoryCorpusSanitizerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectoryCorpusSanitizer {

    private final BatchProcessingConfig batchProcessingConfig;
    private final DirectoryCorpusSanitizerConfig config;

    public void processDirectory() {
        try {
            Files.list(Paths.get(config.getInFilepath()))
                    .filter(path -> !Paths.get(config.getOutFilepath(), path.getFileName().toString()).toFile().exists())
                    .forEach(path -> {
                        CorpusSanitizerConfig corpusSanitizerConfig = new CorpusSanitizerConfig();
                        corpusSanitizerConfig.setInFilepath(path.toAbsolutePath().toString());
                        corpusSanitizerConfig.setOutFilepath(Paths.get(config.getOutFilepath(), path.getFileName().toString()).toAbsolutePath().toString());
                        CorpusSanitizer corpusSanitizer = new CorpusSanitizer(batchProcessingConfig, corpusSanitizerConfig);
                        corpusSanitizer.processFile();

                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
