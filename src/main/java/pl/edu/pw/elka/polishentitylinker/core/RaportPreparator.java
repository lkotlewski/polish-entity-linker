package pl.edu.pw.elka.polishentitylinker.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RaportPreparator {

    private static final String LOG_PATTERN = "%s: %s, %s, %s";
    private static final String LOG_NO_CANDIDATE_PATTERN = "%s: no candidate";

    public static void evaluateSearcherResultsParams(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        int namedEntitiesCount = candidatesForMentions.size();
        int notEmptyCandidatesSetsCount = candidatesForMentions.stream()
                .mapToInt(c -> c.getSecond().isEmpty() ? 0 : 1).sum();
        int withGoodCandidateCount = candidatesForMentions.stream()
                .mapToInt(c -> {
                    List<String> candidatesIds = c.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
                    return candidatesIds.contains(c.getFirst().getEntityId()) ? 1 : 0;
                }).sum();
        int allCandidatesSum = candidatesForMentions.stream()
                .mapToInt(c -> c.getSecond().size()).sum();

        float avCandidatesCount = allCandidatesSum / (float) namedEntitiesCount;
        float precision = withGoodCandidateCount / (float) notEmptyCandidatesSetsCount;
        float recall = withGoodCandidateCount / (float) namedEntitiesCount;

        log.info("###Searcher###");
        log.info(String.format("%d/%d, Mean candidate count: %.4f", allCandidatesSum, namedEntitiesCount, avCandidatesCount));
        log.info(String.format("%d/%d, Precision: %.4f", withGoodCandidateCount, notEmptyCandidatesSetsCount, precision));
        log.info(String.format("%d/%d, Recall: %.4f", withGoodCandidateCount, namedEntitiesCount, recall));

    }

    public static void evaluateDisambiguatorParams(List<WikiItemEntity> chosenEntities, List<Pair<NamedEntity,
            List<WikiItemEntity>>> candidatesForMentions) {
        AtomicInteger withMoreThanOneCandidate = new AtomicInteger(0);
        AtomicInteger withOneCandidate = new AtomicInteger(0);
        AtomicInteger withMoreThanZeroCandidate = new AtomicInteger(0);
        AtomicInteger withGoodProperlyChosen = new AtomicInteger(0);
        AtomicInteger withGoodWronglyChosen = new AtomicInteger(0);
        AtomicInteger withNoGoodProperlyChosen = new AtomicInteger(0);
        AtomicInteger withNoGoodWronglyChosen = new AtomicInteger(0);
        AtomicInteger oneGoodProperlyChosen = new AtomicInteger(0);
        AtomicInteger oneGoodWronglyChosen = new AtomicInteger(0);
        AtomicInteger oneNoGoodProperlyChosen = new AtomicInteger(0);
        AtomicInteger oneNoGoodWronglyChosen = new AtomicInteger(0);

        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            Pair<NamedEntity, List<WikiItemEntity>> tartgetCandidatesPair = candidatesForMentions.get(i);
            NamedEntity targetNamedEntity = tartgetCandidatesPair.getFirst();
            List<String> candidatesIds = tartgetCandidatesPair.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
            if (candidatesIds.size() > 1) {
                withMoreThanOneCandidate.incrementAndGet();
                withMoreThanZeroCandidate.incrementAndGet();
                extracted(withGoodProperlyChosen, withGoodWronglyChosen,
                        withNoGoodProperlyChosen, withNoGoodWronglyChosen,
                        choice, targetNamedEntity, candidatesIds);
            } else if (candidatesIds.size() == 1) {
                withOneCandidate.incrementAndGet();
                withMoreThanZeroCandidate.incrementAndGet();
                extracted(oneGoodProperlyChosen, oneGoodWronglyChosen,
                        oneNoGoodProperlyChosen, oneNoGoodWronglyChosen,
                        choice, targetNamedEntity, candidatesIds);
            }
        });

        int properlyChosen = withGoodProperlyChosen.get() + oneGoodProperlyChosen.get() +
                withNoGoodProperlyChosen.get() + oneNoGoodProperlyChosen.get();
        float disambiguationAccuracy = properlyChosen / (float) withMoreThanZeroCandidate.get();
        log.info("###Disambiguator###");
        log.info(String.format("Good anwers %d/%d, Overall Acurracy: %.4f", properlyChosen, withMoreThanZeroCandidate.get(), disambiguationAccuracy));

        int properlyChosenFromMany = withGoodProperlyChosen.get() + withNoGoodProperlyChosen.get();
        float complexDisambiguationAccuracy = properlyChosenFromMany / (float) withMoreThanOneCandidate.get();
        log.info(String.format("Good complex anwers %d/%d, Complex Acurracy: %.4f", properlyChosenFromMany, withMoreThanOneCandidate.get(), complexDisambiguationAccuracy));
    }

    private static void extracted(AtomicInteger withGoodProperlyChosen,
                           AtomicInteger withGoodWronglyChosen,
                           AtomicInteger withNoGoodProperlyChosen,
                           AtomicInteger withNoGoodWronglyChosen,
                           WikiItemEntity choice, NamedEntity targetNamedEntity,
                           List<String> candidatesIds) {
        if (candidatesIds.contains(targetNamedEntity.getEntityId())) {
            if (choice.getId().equals(targetNamedEntity.getEntityId())) {
                withGoodProperlyChosen.incrementAndGet();
            } else {
                withGoodWronglyChosen.incrementAndGet();
            }
        } else {
            if (choice == null) {
                withNoGoodProperlyChosen.incrementAndGet();
            } else {
                withNoGoodWronglyChosen.incrementAndGet();
            }
        }
    }

    public static void evaluateOverallParams(Path path, List<WikiItemEntity> chosenEntities, List<NamedEntity> referenceEntities) {
        AtomicInteger goodAnswers = new AtomicInteger(0);
        AtomicInteger allAnswers = new AtomicInteger(0);
        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            NamedEntity referenceEntity = referenceEntities.get(i);

            String choiceLog = createChoiceLog(choice, referenceEntity);
            if (choice != null && choice.getId().equals(referenceEntity.getEntityId())) {
                goodAnswers.incrementAndGet();
            }
            allAnswers.incrementAndGet();
            try {
                Files.write(path, (choiceLog + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("", e);
            }
        });

        float accuracy = goodAnswers.get() / (float) allAnswers.get();
        log.info("###Whole system###");
        log.info(String.format("Good anwers %d/%d, Overall Acurracy: %.4f", goodAnswers.get(), allAnswers.get(), accuracy));
    }

    private static String createChoiceLog(WikiItemEntity choice, NamedEntity reference) {
        String choiceLog;
        if (choice != null) {
            choiceLog = String.format(LOG_PATTERN, reference.getEntityId(), choice.getId(), reference.getLinkTitle(), choice.getTitlePl());
        } else {
            choiceLog = String.format(LOG_NO_CANDIDATE_PATTERN, reference.getEntityId());
        }
        return choiceLog;
    }
}
