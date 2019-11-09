package pl.edu.pw.elka.polishentitylinker.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "redirect_page")
public class RedirectPageEntity {

    @Id
    private int id;

    private String label;
}
