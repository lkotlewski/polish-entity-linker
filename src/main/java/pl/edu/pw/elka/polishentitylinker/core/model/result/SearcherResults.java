package pl.edu.pw.elka.polishentitylinker.core.model.result;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearcherResults {

    private float meanPassedCandidatesCount;
    private float precision;
    private float recall;
    private float clearance;
}
