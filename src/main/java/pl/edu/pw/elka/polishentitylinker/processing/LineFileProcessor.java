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
        processLineByLine(Paths.get(path));
    }

    protected void processLineByLine(Path path) {
        try {
            Files.lines(path).forEach(this::processLine);
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
