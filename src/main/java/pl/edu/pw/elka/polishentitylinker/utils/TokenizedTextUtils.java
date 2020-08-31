package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenizedTextUtils {

    private static String INTERP_TAG = "interp:";
    private static String AGLT_TAG_PREFIX = "aglt";

    public static String spanToOriginalForm(List<TokenizedWord> span) {
        StringBuilder s = new StringBuilder();
        for (TokenizedWord tokenizedWord : span) {
            if (appendSpace(s, tokenizedWord)) {
                s.append(" ");
            }
            if (tokenizedWord != null) {
                s.append(tokenizedWord.getToken());
            }
        }

        return s.toString();
    }

    public static String spanToLemmatizedForm(List<TokenizedWord> span) {
        StringBuilder s = new StringBuilder();
        for (TokenizedWord tokenizedWord : span) {
            if (appendSpace(s, tokenizedWord)) {
                s.append(" ");
            }
            if (tokenizedWord instanceof TokenizedExtendedWord) {
                s.append(((TokenizedExtendedWord) tokenizedWord).getLemma());
            }
        }

        return s.toString();
    }

    private static boolean appendSpace(StringBuilder s, TokenizedWord tokenizedWord) {
        return tokenizedWord == null || (isPrecedingSpace(tokenizedWord) && s.length() != 0);
    }

    private static boolean isPrecedingSpace(TokenizedWord tokenizedWord) {
        if (tokenizedWord instanceof TokenizedExtendedWord) {
            String morphosyntacticTags = ((TokenizedExtendedWord) tokenizedWord).getMorphosyntacticTags();
            return morphosyntacticTags != null &&
                    !morphosyntacticTags.startsWith(AGLT_TAG_PREFIX) && !INTERP_TAG.equals(morphosyntacticTags);
        }
        return tokenizedWord.isPrecedingSpace();
    }
}
