package pl.edu.pw.elka.polishentitylinker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;
import pl.edu.pw.elka.polishentitylinker.utils.CandidateSearchUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NaiveSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        String lemma = namedEntity.getEntitySpan().get(0).getLemma();
        List<WikiItemEntity> candidates = wikiItemRepository.findAllSimilarToLemmaFromRedirects(lemma);
        candidates.addAll(wikiItemRepository.findAllSimilarToLemma(lemma));
        return CandidateSearchUtils.getDistinctRegularArticles(candidates);
    }
}
