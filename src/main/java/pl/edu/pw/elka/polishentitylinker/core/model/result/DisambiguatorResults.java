package pl.edu.pw.elka.polishentitylinker.core.model.result;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DisambiguatorResults {

    private float complexDisambiguationAccuracy;
}
