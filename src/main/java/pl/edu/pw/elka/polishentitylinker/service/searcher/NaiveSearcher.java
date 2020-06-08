package pl.edu.pw.elka.polishentitylinker.service.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.CandidateSearchUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NaiveSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        TokenizedWord firstWorld = namedEntity.getEntitySpan().get(0);
        String lemma;
        if (firstWorld instanceof TokenizedExtendedWord) {
            lemma = ((TokenizedExtendedWord) firstWorld).getLemma();
        } else {
            lemma = firstWorld.getToken();
        }
        List<WikiItemEntity> candidates = wikiItemRepository.findAllSimilarToLemmaFromRedirects(lemma);
        candidates.addAll(wikiItemRepository.findAllSimilarToLemma(lemma));
        return CandidateSearchUtils.getDistinctRegularArticles(candidates);
    }
}
