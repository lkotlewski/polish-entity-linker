package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;

import java.util.List;

@Data
public class NamedEntity {

    private List<TaggedWord> entitySpan;
    private Context context;
}
