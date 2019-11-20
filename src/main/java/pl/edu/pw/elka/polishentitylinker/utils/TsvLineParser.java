package pl.edu.pw.elka.polishentitylinker.utils;

import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.util.Arrays;
import java.util.List;

public class TsvLineParser {

    public static TokenizedWord parseTokenizedWord(String line) {
        List<String> parts = Arrays.asList(line.split("\\t"));
        if (parts.size() == 5) {
            return TokenizedWord.builder()
                    .docId(Integer.parseInt(parts.get(0)))
                    .token(parts.get(1))
                    .precedingSpace("1".equals(parts.get(2)))
                    .linkTitle(parts.get(3))
                    .entityId(parts.get(4))
                    .build();
        }
        return null;
    }

    public static TokenizedExtendedWord parseTokenizedExtendedWord(String line) {
        List<String> parts = Arrays.asList(line.split("\\s"));
        if (parts.size() == 7) {
            return TokenizedExtendedWord.builder()
                    .docId(Integer.parseInt(parts.get(0)))
                    .token(parts.get(1))
                    .lemma(parts.get(2))
                    .precedingSpace("1".equals(parts.get(3)))
                    .morphosyntacticTags(parts.get(4))
                    .linkTitle(parts.get(5))
                    .entityId(parts.get(6))
                    .build();
        }
        return null;
    }
}
