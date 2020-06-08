package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.utils.TokenizedTextUtils;

import java.util.List;

@Data
public class NamedEntity {

    private List<TokenizedWord> entitySpan;
    private List<TokenizedWord> context;

    public String toOriginalForm() {
        return TokenizedTextUtils.spanToOriginalForm(entitySpan);
    }

    public String getContextAsString() {
        return TokenizedTextUtils.spanToOriginalForm(context);
    }

    public String getEntityId() {
        return entitySpan.get(0).getEntityId();
    }

    public String getLinkTitle() {return entitySpan.get(0).getLinkTitle();}

    public Integer getPageId() {
        return entitySpan.get(0).getDocId();
    }
}
