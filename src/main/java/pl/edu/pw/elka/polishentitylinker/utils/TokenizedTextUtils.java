package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenizedTextUtils {

    public static String spanToOriginalForm(List<TokenizedWord> span) {
        StringBuilder s = new StringBuilder();
        span.forEach(tokenizedWord -> {
                    if (tokenizedWord == null || (tokenizedWord.isPrecedingSpace() && s.length() != 0)) {
                        s.append(" ");
                    }
                    if (tokenizedWord != null) {
                        s.append(tokenizedWord.getToken());
                    }
                }
        );
        return s.toString();
    }

    public static String spanToLemmatizedForm(List<TokenizedWord> span) {
        StringBuilder s = new StringBuilder();
        span.forEach(tokenizedWord -> {
                    if (tokenizedWord == null || (tokenizedWord.isPrecedingSpace() && s.length() != 0)) {
                        s.append(" ");
                    }
                    if (tokenizedWord instanceof TokenizedExtendedWord) {
                        s.append(((TokenizedExtendedWord) tokenizedWord).getLemma());
                    }
                }
        );
        return s.toString();
    }
}
