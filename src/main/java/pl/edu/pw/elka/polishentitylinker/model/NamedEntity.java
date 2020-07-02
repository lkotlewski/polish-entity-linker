package pl.edu.pw.elka.polishentitylinker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.utils.TokenizedTextUtils;

import java.util.List;

@Data
public class NamedEntity {

    private List<TokenizedWord> entitySpan;
    private List<TokenizedWord> context;

    @JsonIgnore
    public String toOriginalForm() {
        return TokenizedTextUtils.spanToOriginalForm(entitySpan);
    }

    @JsonIgnore
    public String getContextAsString() {
        return TokenizedTextUtils.spanToOriginalForm(context);
    }

    @JsonIgnore
    public String getEntityId() {
        return entitySpan.get(0).getEntityId();
    }

    @JsonIgnore
    public String getLinkTitle() {
        return entitySpan.get(0).getLinkTitle();
    }

    @JsonIgnore
    public Integer getPageId() {
        return entitySpan.get(0).getDocId();
    }
}
