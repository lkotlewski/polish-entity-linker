package pl.edu.pw.elka.polishentitylinker.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "alias_lemmatized")
public class AliasLemmatizedEntity {

    @Id
    @GeneratedValue
    private int id;

    private String label;

    @ManyToOne
    private WikiItemEntity target;
}
