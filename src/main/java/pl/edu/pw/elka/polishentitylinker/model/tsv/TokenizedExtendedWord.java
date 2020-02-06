package pl.edu.pw.elka.polishentitylinker.model.tsv;

import lombok.Builder;
import lombok.Data;

@Data
public class TokenizedExtendedWord extends TokenizedWord {

    private String lemma;
    private String morphosyntacticTags;

    @Builder(builderMethodName = "extendedBuilder")
    public TokenizedExtendedWord(Integer docId, String token, boolean precedingSpace, String linkTitle, String entityId, String lemma, String morphosyntacticTags) {
        super(docId, token, precedingSpace, linkTitle, entityId);
        this.lemma = lemma;
        this.morphosyntacticTags = morphosyntacticTags;
    }
}
