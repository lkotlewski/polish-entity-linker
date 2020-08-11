package pl.edu.pw.elka.polishentitylinker.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.core.model.result.DisambiguatorResults;
import pl.edu.pw.elka.polishentitylinker.core.model.result.SearcherResults;
import pl.edu.pw.elka.polishentitylinker.core.model.result.WholeSystemResults;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RaportPreparator {

    private static final String LOG_PATTERN = "%s: %s, %s, %s";
    private static final String LOG_NO_CANDIDATE_PATTERN = "%s: no candidate";

    public static SearcherResults evaluateSearcherResultsParams(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        int namedEntitiesCount = candidatesForMentions.size();
        int notEmptyCandidatesSetsCount = candidatesForMentions.stream()
                .mapToInt(c -> c.getSecond().isEmpty() ? 0 : 1).sum();
        int withGoodCandidateCount = candidatesForMentions.stream()
                .mapToInt(c -> {
                    List<String> candidatesIds = c.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
                    return candidatesIds.contains(c.getFirst().getEntityId()) ? 1 : 0;
                }).sum();
        int withOnlyGoodCandidateCount = candidatesForMentions.stream()
                .mapToInt(c -> {
                    List<String> candidatesIds = c.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
                    return candidatesIds.size() == 1 && candidatesIds.contains(c.getFirst().getEntityId()) ? 1 : 0;
                }).sum();
        int passedToDisambiguateCount = candidatesForMentions.stream()
                .mapToInt(c -> {
                    List<String> candidatesIds = c.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
                    return candidatesIds.size() > 1 && candidatesIds.contains(c.getFirst().getEntityId()) ? 1 : 0;
                }).sum();
        int passedToDisambiguateSum = candidatesForMentions.stream()
                .mapToInt(c -> {
                    List<String> candidatesIds = c.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
                    return candidatesIds.size() > 1 && candidatesIds.contains(c.getFirst().getEntityId()) ? candidatesIds.size() : 0;
                }).sum();

        float meanPassedCandidatesCount = passedToDisambiguateSum / (float) passedToDisambiguateCount;
        float precision = withGoodCandidateCount / (float) notEmptyCandidatesSetsCount;
        float recall = withGoodCandidateCount / (float) namedEntitiesCount;
        float clearance = withOnlyGoodCandidateCount / (float) namedEntitiesCount;

        log.info("###Searcher###");
        log.info(String.format("%d/%d, Mean candidate passed count: %.4f", passedToDisambiguateSum, passedToDisambiguateCount, meanPassedCandidatesCount));
        log.info(String.format("%d/%d, Precision: %.4f", withGoodCandidateCount, notEmptyCandidatesSetsCount, precision));
        log.info(String.format("%d/%d, Recall: %.4f", withGoodCandidateCount, namedEntitiesCount, recall));
        log.info(String.format("%d/%d, Clearance: %.4f", withOnlyGoodCandidateCount, namedEntitiesCount, clearance));

        return SearcherResults
                .builder()
                .meanPassedCandidatesCount(meanPassedCandidatesCount)
                .precision(precision)
                .recall(recall)
                .clearance(clearance)
                .build();

    }

    public static DisambiguatorResults evaluateDisambiguatorParams(List<WikiItemEntity> chosenEntities, List<Pair<NamedEntity,
            List<WikiItemEntity>>> candidatesForMentions) {
        AtomicInteger withMoreThanOneCandidate = new AtomicInteger(0);
        AtomicInteger properlyChosenFromMany = new AtomicInteger(0);
        AtomicInteger wronglyChosenFromMany = new AtomicInteger(0);

        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            Pair<NamedEntity, List<WikiItemEntity>> targetCandidatesPair = candidatesForMentions.get(i);
            NamedEntity targetNamedEntity = targetCandidatesPair.getFirst();
            List<String> candidatesIds = targetCandidatesPair.getSecond().stream().map(WikiItemEntity::getId).collect(Collectors.toList());
            if (candidatesIds.contains(targetNamedEntity.getEntityId())) {
                if (candidatesIds.size() > 1) {
                    withMoreThanOneCandidate.incrementAndGet();
                    incrementCounts(properlyChosenFromMany, wronglyChosenFromMany,
                            choice, targetNamedEntity);
                }
            }
        });

        log.info("###Disambiguator###");
        float complexDisambiguationAccuracy = properlyChosenFromMany.get() / (float) withMoreThanOneCandidate.get();
        log.info(String.format("Good complex anwers %d/%d, Complex Acurracy: %.4f", properlyChosenFromMany.get(), withMoreThanOneCandidate.get(), complexDisambiguationAccuracy));
        return DisambiguatorResults.builder()
                .complexDisambiguationAccuracy(complexDisambiguationAccuracy)
                .build();
    }

    private static void incrementCounts(AtomicInteger properlyChosen, AtomicInteger wronglyChosen,
                                        WikiItemEntity choice, NamedEntity targetNamedEntity) {
        if (choice.getId().equals(targetNamedEntity.getEntityId())) {
            properlyChosen.incrementAndGet();
        } else {
            wronglyChosen.incrementAndGet();
        }
    }

    public static WholeSystemResults evaluateOverallParams(List<WikiItemEntity> chosenEntities, List<NamedEntity> referenceEntities) {
        AtomicInteger goodAnswers = new AtomicInteger(0);
        AtomicInteger allAnswers = new AtomicInteger(0);
        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            NamedEntity referenceEntity = referenceEntities.get(i);

            if (choice != null && choice.getId().equals(referenceEntity.getEntityId())) {
                goodAnswers.incrementAndGet();
            }
            allAnswers.incrementAndGet();
        });

        float accuracy = goodAnswers.get() / (float) allAnswers.get();
        log.info("###Whole system###");
        log.info(String.format("Good anwers %d/%d, Overall Acurracy: %.4f", goodAnswers.get(), allAnswers.get(), accuracy));
        return WholeSystemResults.builder()
                .accuracy(accuracy)
                .build();
    }

    public static void saveChoicesToFile(Path choicesPath, List<WikiItemEntity> chosenEntities, List<NamedEntity> referenceEntities) {
        IntStream.range(0, chosenEntities.size()).forEach(i -> {
            WikiItemEntity choice = chosenEntities.get(i);
            NamedEntity referenceEntity = referenceEntities.get(i);
            String choiceLog = createChoiceLog(choice, referenceEntity);
            try {
                Files.write(choicesPath, (choiceLog + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("", e);
            }
        });
    }

    public static List<Pair<NamedEntity, List<WikiItemEntity>>> withCandidatesDeepCopy(List<Pair<NamedEntity, List<WikiItemEntity>>> listToCopy) {
        List<Pair<NamedEntity, List<WikiItemEntity>>> copy = new ArrayList<>();
        listToCopy.forEach(item -> copy.add(Pair.of(item.getFirst(), deepCopyList(item.getSecond()))));
        return copy;
    }

    public static <T> List<T> deepCopyList(List<T> listToCopy) {
        List<T> copy = new ArrayList<>();
        listToCopy.forEach(copy::add);
        return copy;
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
