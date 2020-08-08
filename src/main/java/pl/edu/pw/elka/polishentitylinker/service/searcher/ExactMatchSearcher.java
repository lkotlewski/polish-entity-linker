package pl.edu.pw.elka.polishentitylinker.service.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExactMatchSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {
        return wikiItemRepository.findAllByOriginalFormFromAliases(namedEntity.toOriginalForm());
    }
}
