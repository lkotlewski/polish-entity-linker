package pl.edu.pw.elka.polishentitylinker.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.service.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityLinker {

    private final String LOG_PATTERN = "%s: %s, %s, %s";
    private final String LOG_NO_CANDIDATE_PATTERN = "%s: no candidate";

    private final EntityLinkerConfig config;
    private final Searcher searcher;
    private final Disambiguator disambiguator;

    private final TaggedTextIterator taggedTextIterator = new TaggedTextIterator();

    public void linkEntities() {
        taggedTextIterator.processFile(config.getTestFilepath());
        taggedTextIterator.getNamedEntities()
                .forEach(namedEntity -> {
                    List<WikiItemEntity> candidates = searcher.findCandidates(namedEntity);
                    WikiItemEntity choice = disambiguator.choose(namedEntity, candidates);
                    if(choice != null) {
                        System.out.println(String.format(LOG_PATTERN, namedEntity.getEntitySpan().get(0).getEntityId(), choice.getId(), choice.getLabelPl(), choice.getTitlePl()));
                    } else {
                        System.out.println(String.format(LOG_NO_CANDIDATE_PATTERN, namedEntity.getEntitySpan().get(0).getEntityId()));
                    }
                });
    }




}
