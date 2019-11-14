package pl.edu.pw.elka.polishentitylinker.imports;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Slf4j
public abstract class LineFileProcessor {

     void parseFile(String path, Consumer<String> action) {
        Path path1 = Paths.get(path);

        try {
            Files.lines(path1).forEach(action);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public abstract void parseFile();

}
