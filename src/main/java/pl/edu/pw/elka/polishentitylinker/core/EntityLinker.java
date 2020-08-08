package pl.edu.pw.elka.polishentitylinker.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.config.EntityLinkerConfig;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.service.searcher.Searcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static pl.edu.pw.elka.polishentitylinker.core.utils.EntityLinkerUtils.*;
import static pl.edu.pw.elka.polishentitylinker.core.utils.RaportPreparator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityLinker {

    private final EntityLinkerConfig config;
    private final Searcher searcher;
    private final Disambiguator disambiguator;
    private final WikiItemRepository wikiItemRepository;
    private final ObjectMapper objectMapper;

    private final TaggedTextIterator taggedTextIterator = new TaggedTextIterator();

    public void linkEntities() {
        Path choicesFilepath = Paths.get(config.getOutFilepath());
        Path candidatesFilepath = Paths.get(config.getCandidatesFilepath());
        taggedTextIterator.processFile(config.getTestFilepath());
        List<NamedEntity> namedEntities = taggedTextIterator.getNamedEntities();

        filterByNotDisambiguationPage(namedEntities);

        log.info("{} entities to process", namedEntities.size());
        List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions;
        if (config.isDoSearch()) {
            candidatesForMentions = searchForCandidates(namedEntities);
            saveSearcherResults(candidatesFilepath, candidatesForMentions, objectMapper);
        } else {
            candidatesForMentions = readSearcherResults(candidatesFilepath, objectMapper);
        }

        List<Pair<NamedEntity, List<WikiItemEntity>>> emptyCandidates = candidatesForMentions.stream().filter(pair -> pair.getSecond().isEmpty()).collect(Collectors.toList());
        List<Pair<NamedEntity, List<WikiItemEntity>>> containingNoGoodCandidate = candidatesForMentions.stream().filter(pair -> !pair.getSecond().isEmpty() && !containsGoodCandidate(pair.getSecond(), pair.getFirst())).collect(Collectors.toList());

        if (config.isLimitSearchResults()) {
            limitSearchResults(candidatesForMentions, config.getSearchResultsLimit());
        }

        if (config.isDoDisambiguate()) {
            clearCandidatesContainingNoGood(candidatesForMentions);
            List<WikiItemEntity> chosenEntities = disambiguate(candidatesForMentions);
            List<NamedEntity> referenceEntities = candidatesForMentions.stream().map(Pair::getFirst).collect(Collectors.toList());
            if (chosenEntities.size() != referenceEntities.size()) {
                throw new IllegalStateException("Result list size is different from size of entities to disambiguate list");
            }
            evaluateSearcherResultsParams(candidatesForMentions);
            evaluateDisambiguatorParams(chosenEntities, candidatesForMentions);
            evaluateOverallParams(chosenEntities, referenceEntities);
            saveChoicesToFile(choicesFilepath, chosenEntities, referenceEntities);
            log.info("All candidates Chosen");
        } else {
            evaluateSearcherResultsParams(candidatesForMentions);
            clearCandidatesContainingNoGood(candidatesForMentions);
            int allCandidatesSum = candidatesForMentions.stream().mapToInt(c -> c.getSecond().size()).sum();
            int al = 0;
        }
    }

    private List<Pair<NamedEntity, List<WikiItemEntity>>> searchForCandidates(List<NamedEntity> namedEntities) {
        List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions = new ArrayList<>();
        int allNamedEntitiesSize = namedEntities.size();
        AtomicInteger processedSize = new AtomicInteger(0);
        namedEntities.forEach(namedEntity -> {
            List<WikiItemEntity> candidates = searcher.findCandidates(namedEntity);
            candidatesForMentions.add(Pair.of(namedEntity, candidates));
            log.info("{}/{} candidates list generated", processedSize.incrementAndGet(), allNamedEntitiesSize);
        });
        return candidatesForMentions;
    }

    private List<WikiItemEntity> disambiguate(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        return disambiguator.chooseAll(candidatesForMentions);
    }

    private void filterByNotDisambiguationPage(List<NamedEntity> namedEntities) {
        namedEntities.removeIf(namedEntity -> {
                    Optional<WikiItemEntity> byId = wikiItemRepository.findById(namedEntity.getEntityId());
                    return !byId.isPresent() || byId.get().getPageType() == PageType.DISAMBIGUATION;
                }
        );
    }
}
