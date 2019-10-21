package pl.edu.pw.elka.polishentitylinker.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity(name = "wikiItem")
@Getter
@Setter
public class WikiItemEntity {

    @Id
    private String id;

    private String labelEng;

    private String labelPl;

    @Column(unique=true)
    private String titlePl;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<WikiItemEntity> instanceOf;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<WikiItemEntity> subclassOf;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<WikiItemEntity> rootParentCategories;
}
