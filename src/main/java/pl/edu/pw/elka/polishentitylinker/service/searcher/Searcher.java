package pl.edu.pw.elka.polishentitylinker.service.searcher;

import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.List;

@Service
public interface Searcher {

    List<WikiItemEntity> findCandidates(NamedEntity namedEntity);
}
