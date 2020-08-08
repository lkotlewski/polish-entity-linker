package pl.edu.pw.elka.polishentitylinker.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

@Data
@AllArgsConstructor
public class CandidateWithContextMatch {

    private WikiItemEntity wikiItemEntity;
    private Double contextMatch;
}
