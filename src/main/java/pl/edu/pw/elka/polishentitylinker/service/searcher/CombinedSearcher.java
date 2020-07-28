package pl.edu.pw.elka.polishentitylinker.service.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.List;

@Primary
@Service
@RequiredArgsConstructor
public class CombinedSearcher implements Searcher {

    private final ExactMatchSearcher exactMatchSearcher;
    private final ExactMatchLemmatizedSearcher exactMatchLemmatizedSearcher;
    private final PartLikeMatchSearcher partLikeMatchSearcher;
    private final PartLikeLemmaMatchSearcher partLikeLemmaMatchSearcher;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        List<WikiItemEntity> candidates = exactMatchSearcher.findCandidates(namedEntity);
        if (candidates.isEmpty()) {
            candidates = exactMatchLemmatizedSearcher.findCandidates(namedEntity);
        }
        if (candidates.isEmpty()) {
            candidates = partLikeMatchSearcher.findCandidates(namedEntity);
        }
        if (candidates.isEmpty()) {
            candidates = partLikeLemmaMatchSearcher.findCandidates(namedEntity);
        }
        return candidates;
    }
}
