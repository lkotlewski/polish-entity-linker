package pl.edu.pw.elka.polishentitylinker.core.model.result;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultForConfiguration {

    private float popularityRate;
    private int candidatesLimit;
    private SearcherResults searcherResults;
    private DisambiguatorResults disambiguatorResults;
    private WholeSystemResults wholeSystemResults;
}
