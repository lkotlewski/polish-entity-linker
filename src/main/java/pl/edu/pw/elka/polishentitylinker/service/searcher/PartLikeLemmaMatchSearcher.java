package pl.edu.pw.elka.polishentitylinker.service.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.TokenizedTextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PartLikeLemmaMatchSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        List<TokenizedWord> enititySpan = new ArrayList<>(namedEntity.getEntitySpan());

        while (enititySpan.size() > 0) {
            List<WikiItemEntity> candidates = wikiItemRepository.findAllByLemmatizedFormSimilarFromLemmatizedAliases(
                    TokenizedTextUtils.spanToLemmatizedForm(enititySpan));
            if (!candidates.isEmpty()) {
                return candidates;
            }
            enititySpan = enititySpan.subList(1, enititySpan.size());
        }
        return Collections.emptyList();
    }
}
