package pl.edu.pw.elka.polishentitylinker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CombinedSearcher implements Searcher {

    private final NaiveSearcher naiveSearcher;
    private final ExactMatchSearcher exactMatchSearcher;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        List<WikiItemEntity> candidates = exactMatchSearcher.findCandidates(namedEntity);
        return candidates.isEmpty() ? naiveSearcher.findCandidates(namedEntity) : candidates;
    }
}
