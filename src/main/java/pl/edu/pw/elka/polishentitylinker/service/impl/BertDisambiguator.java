package pl.edu.pw.elka.polishentitylinker.service.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.Disambiguator;

import java.util.List;

@Primary
@Service
public class BertDisambiguator implements Disambiguator {

    @Override
    public WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates) {
        return null;
    }
}
