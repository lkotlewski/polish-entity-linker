package pl.edu.pw.elka.polishentitylinker.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "redirect_page")
public class RedirectPageEntity {

    @Id
    private int id;

    private String label;

    @ManyToOne
    private WikiItemEntity target;
}
