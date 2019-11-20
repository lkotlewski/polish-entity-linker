package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;

import java.util.List;

@Data
public class NamedEntity {

    private List<TokenizedExtendedWord> entitySpan;
    private Context context;

    public String toOriginalForm() {
        StringBuilder s = new StringBuilder();
        entitySpan.stream()
                .forEach(tokenizedWord -> {
                            if (tokenizedWord.isPrecedingSpace() && s.length() != 0) {
                                s.append(" ");
                            }
                            s.append(tokenizedWord.getToken());
                        }
                );
        return s.toString();
    }
}
