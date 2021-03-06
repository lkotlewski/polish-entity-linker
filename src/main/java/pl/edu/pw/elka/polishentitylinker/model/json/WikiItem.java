package pl.edu.pw.elka.polishentitylinker.model.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiItem {
    private String id;
    private String labelEng;
    private String labelPl;
    private String titlePl;
    private List<String> instanceOf;
    private List<String> subclassOf;

    @JsonCreator
    public WikiItem(@JsonProperty("id") String id, @JsonProperty("labels") Label label, @JsonProperty("wiki") WikiTitle wikiTitle,
                    @JsonProperty("P31") List<String> instanceOf, @JsonProperty("P279") List<String> subclassOf) {
        this.id = id;
        this.labelEng = label.getEn();
        this.labelPl = label.getPl();
        this.titlePl = wikiTitle.getPl();
        this.instanceOf = instanceOf;
        this.subclassOf = subclassOf;
    }
}
