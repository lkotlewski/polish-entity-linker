package pl.edu.pw.elka.polishentitylinker.processing;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public abstract class LineFileProcessor implements FileProcessor {

    protected abstract void processLine(String line);

    protected void processLineByLine(String path) {
        Path path1 = Paths.get(path);

        try {
            Files.lines(path1).forEach(this::processLine);
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
