package pl.edu.pw.elka.polishentitylinker.service.disambiguator;

import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.List;

public interface Disambiguator {

    WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates);

    List<WikiItemEntity> chooseAll(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions);
}
