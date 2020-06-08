package pl.edu.pw.elka.polishentitylinker.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.service.searcher.Searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private int goodAnswers = 0;
    private int allAnswers = 0;

    public void linkEntities() {
        Path path = Paths.get(config.getOutFilepath());
        taggedTextIterator.processFile(config.getTestFilepath());
        List<NamedEntity> namedEntities = taggedTextIterator.getNamedEntities();
        log.info("{} entities to disambiguate", namedEntities.size());
        disambiguate(path, searchForCandidates(namedEntities));
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

    private void disambiguate(Path path, List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        List<WikiItemEntity> chosenEntities = disambiguator.chooseAll(candidatesForMentions);
        List<NamedEntity> referenceEntities = candidatesForMentions.stream().map(Pair::getFirst).collect(Collectors.toList());

        if (chosenEntities.size() != referenceEntities.size()) {
            throw new IllegalStateException("Result list size is different from size of entities to disambiguate list");
        }
        log.info("All candidates disambiguated");
        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            NamedEntity referenceEntity = referenceEntities.get(i);

            String choiceLog = createChoiceLog(choice, referenceEntity);
            if (choice != null && choice.getId().equals(referenceEntity.getEntityId())) {
                goodAnswers++;
            }
            allAnswers++;
            try {
                Files.write(path, (choiceLog + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("", e);
            }
        });

        float accuracy = goodAnswers / (float) allAnswers;
        log.info(String.format("Good anwers %d/%d, Acurracy: %.4f", goodAnswers, allAnswers, accuracy));
    }

    private String createChoiceLog(WikiItemEntity choice, NamedEntity reference) {
        String choiceLog;
        if (choice != null) {
            choiceLog = String.format(LOG_PATTERN, reference.getEntityId(), choice.getId(), reference.getLinkTitle(), choice.getTitlePl());
        } else {
            choiceLog = String.format(LOG_NO_CANDIDATE_PATTERN, reference.getEntityId());
        }
        return choiceLog;
    }


}
