package pl.edu.pw.elka.polishentitylinker.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DividedArticle {

    byte[] trainPart;
    byte[] testPart;
}
