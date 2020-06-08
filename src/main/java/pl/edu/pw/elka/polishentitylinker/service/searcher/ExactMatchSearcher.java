package pl.edu.pw.elka.polishentitylinker.service.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.CandidateSearchUtils;

import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class ExactMatchSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        return CandidateSearchUtils.getDistinctRegularArticles(wikiItemRepository.findAllByOriginalFormFromAliases(namedEntity.toOriginalForm()));
    }
}
