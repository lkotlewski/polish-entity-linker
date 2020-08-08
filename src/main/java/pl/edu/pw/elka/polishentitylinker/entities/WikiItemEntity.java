package pl.edu.pw.elka.polishentitylinker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;

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

    @Column(unique = true)
    private String titlePl;

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<WikiItemEntity> instanceOf;

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<WikiItemEntity> subclassOf;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private WikiItemEntity rootCategory;

    @Column(unique = true)
    private Integer pageId;

    @Enumerated
    private PageType pageType;

    private Integer mentionsCount;

    private Integer articleLength;

    @JsonIgnore
    public int getNonNullMentionsCount() {
        return this.getMentionsCount() == null ? 0 : this.getMentionsCount();
    }
}
