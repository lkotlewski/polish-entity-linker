package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;

import java.util.List;

@Data
public class Context {
    private List<TokenizedExtendedWord> wordsBefore;
    private List<TokenizedExtendedWord> wordsAfter;
}
