package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TaggedWord {

    long docId;
    String token;
    String lemma;
    boolean precedingSpace;
    String morphosyntacticTags;
    String linkTitle;
    String entityId;
}
