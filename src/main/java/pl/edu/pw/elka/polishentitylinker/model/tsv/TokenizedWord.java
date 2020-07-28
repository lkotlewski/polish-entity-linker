package pl.edu.pw.elka.polishentitylinker.model.tsv;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TokenizedExtendedWord.class, name = "tokenizedExtendedWord")
})
public class TokenizedWord {

    private Integer docId;
    private String token;
    private boolean precedingSpace;
    private String linkTitle;
    private String entityId;
}
