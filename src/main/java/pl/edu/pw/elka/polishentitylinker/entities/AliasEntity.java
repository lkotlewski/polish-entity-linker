package pl.edu.pw.elka.polishentitylinker.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "alias")
public class AliasEntity {

    @Id
    @GeneratedValue
    private int id;

    private String label;

    @ManyToOne
    private WikiItemEntity target;
}
