package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.util.List;

@Data
public class NamedEntity {

    private List<TokenizedWord> entitySpan;
    private List<TokenizedWord> context;

    public String toOriginalForm() {
        return toOriginalForm(entitySpan);
    }

    public String getContextAsString() {
        return toOriginalForm(context);
    }

    private String toOriginalForm(List<TokenizedWord> span) {
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
}
