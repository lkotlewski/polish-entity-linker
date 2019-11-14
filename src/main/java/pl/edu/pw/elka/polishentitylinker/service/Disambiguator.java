package pl.edu.pw.elka.polishentitylinker.service;

import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.List;

public interface Disambiguator {

    public WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates);
}
