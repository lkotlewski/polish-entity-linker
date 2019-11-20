package pl.edu.pw.elka.polishentitylinker.model.tsv;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TokenizedExtendedWord {

    private long docId;
    private String token;
    private String lemma;
    private boolean precedingSpace;
    private String morphosyntacticTags;
    private String linkTitle;
    private String entityId;
}
