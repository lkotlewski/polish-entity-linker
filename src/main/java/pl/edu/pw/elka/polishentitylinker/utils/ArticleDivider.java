package pl.edu.pw.elka.polishentitylinker.utils;

import pl.edu.pw.elka.polishentitylinker.model.DividedArticle;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class ArticleDivider {

    public static DividedArticle divideArticle(Path path, float trainPart) throws IOException {
        List<String> lines = Files.readAllLines(path);

        int halfIndex = (int) (lines.size() * trainPart);
        List<String> secondHalf = lines.subList(halfIndex, lines.size());
        int divideOffset;

        OptionalInt firstEmptyInSecondHalf = IntStream.range(0, secondHalf.size())
                .filter(idx -> secondHalf.get(idx) == null || secondHalf.get(idx).trim().isEmpty())
                .findFirst();
        if (firstEmptyInSecondHalf.isPresent()) {
            divideOffset = firstEmptyInSecondHalf.getAsInt();
        } else {
            divideOffset = IntStream.range(0, secondHalf.size())
                    .filter(idx -> ".".equals(TsvLineParser.parseTokenizedWord(secondHalf.get(idx)).getToken()))
                    .findFirst()
                    .orElse(0);
        }

        int divideIdx = halfIndex + divideOffset;
        DividedArticle dividedArticle = new DividedArticle();

        dividedArticle.setTrainPart(String.join("\n", lines.subList(0, divideIdx)).getBytes());
        dividedArticle.setTestPart(String.join("\n", lines.subList(divideIdx + 1, lines.size())).getBytes());

        return dividedArticle;
    }
}
