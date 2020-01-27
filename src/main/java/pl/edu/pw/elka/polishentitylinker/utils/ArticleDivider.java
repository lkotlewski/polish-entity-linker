package pl.edu.pw.elka.polishentitylinker.utils;

import pl.edu.pw.elka.polishentitylinker.model.DividedArticle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

public class ArticleDivider {

    public static DividedArticle divideArticle(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);

        int halfIndex = lines.size() / 2;
        List<String> secondHalf = lines.subList(halfIndex, lines.size());
        int firstEmptyInSecondHalf = IntStream.range(0, secondHalf.size())
                .filter(idx -> secondHalf.get(idx) == null || secondHalf.get(idx).trim().isEmpty())
                .findFirst()
                .getAsInt();
        int divideIdx = halfIndex + firstEmptyInSecondHalf;
        DividedArticle dividedArticle = new DividedArticle();

        dividedArticle.setTrainPart(String.join("\n", lines.subList(0, divideIdx)).getBytes());
        dividedArticle.setTestPart(String.join("\n", lines.subList(divideIdx + 1, lines.size())).getBytes());

        return dividedArticle;
    }
}
