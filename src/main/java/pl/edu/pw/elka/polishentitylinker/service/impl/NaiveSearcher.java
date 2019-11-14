package pl.edu.pw.elka.polishentitylinker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.TaggedWord;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NaiveSearcher implements Searcher {

    private final WikiItemRepository wikiItemRepository;

    @Override
    public List<WikiItemEntity> findCandidates(NamedEntity namedEntity) {

        return wikiItemRepository.findAllByLemma(namedEntity.getEntitySpan().get(0).getLemma());
    }
}
