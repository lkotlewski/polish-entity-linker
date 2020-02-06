package pl.edu.pw.elka.polishentitylinker.model.tsv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TokenizedWord {

    private Integer docId;
    private String token;
    private boolean precedingSpace;
    private String linkTitle;
    private String entityId;
}
