package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Data;

import java.util.List;

@Data
public class Context {
    private List<TaggedWord> wordsBefore;
    private List<TaggedWord> wordsAfter;
}
