package pl.edu.pw.elka.polishentitylinker.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.service.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.service.Searcher;

import java.io.IOException;
import java.nio.file.*;
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

    private int goodAnswers = 0;
    private int allAnswers = 0;

    public void linkEntities() {
        Path path = Paths.get(config.getOutFilepath());
        taggedTextIterator.processFile(config.getTestFilepath());
        List<NamedEntity> namedEntities = taggedTextIterator.getNamedEntities();
        namedEntities.forEach(namedEntity -> {
            List<WikiItemEntity> candidates = searcher.findCandidates(namedEntity);
            WikiItemEntity choice = disambiguator.choose(namedEntity, candidates);
            String choiceLog;
            TokenizedWord tokenizedWord = namedEntity.getEntitySpan().get(0);
            if (choice != null) {
                choiceLog = String.format(LOG_PATTERN, tokenizedWord.getEntityId(), choice.getId(), tokenizedWord.getLinkTitle(), choice.getTitlePl());
            } else {
                choiceLog = String.format(LOG_NO_CANDIDATE_PATTERN, tokenizedWord.getEntityId());
            }
            log.info(choiceLog);
            if (choice != null && choice.getId().equals(tokenizedWord.getEntityId())) {
                goodAnswers++;
            }
            allAnswers++;
            float accuracy = goodAnswers / (float)allAnswers;
            log.info(String.format("Good anwers %d/%d, Acurracy: %.2f", goodAnswers, allAnswers, accuracy));
            try {
                Files.write(path, (choiceLog + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("", e);
            }
        });
    }


}
