package pl.edu.pw.elka.polishentitylinker.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.core.model.CandidateWithContextMatch;
import pl.edu.pw.elka.polishentitylinker.core.model.CandidatesForMentions;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityLinkerUtils {


    public static void saveSearcherResults(Path candidatesFilepath, List<Pair<NamedEntity,
            List<WikiItemEntity>>> candidatesForMentions, ObjectMapper objectMapper) {
        try {
            CandidatesForMentions candidatesForMentionsBoxed = new CandidatesForMentions(candidatesForMentions);
            objectMapper.writeValue(candidatesFilepath.toFile(), candidatesForMentionsBoxed);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static List<Pair<NamedEntity, List<WikiItemEntity>>> readSearcherResults(Path candidatesFilepath,
                                                                                    ObjectMapper objectMapper) {
        List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions;
        try {
            CandidatesForMentions candidatesForMentionsBoxed = objectMapper.readValue(candidatesFilepath.toFile(),
                    CandidatesForMentions.class);
            candidatesForMentions = candidatesForMentionsBoxed.getPaired();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return candidatesForMentions;
    }

    public static void saveToFile(Path candidatesFilepath, Object object, ObjectMapper objectMapper) {
        try {
            objectMapper.writeValue(candidatesFilepath.toFile(), object);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static  <T>  T readSearcherResults(Path candidatesFilepath, ObjectMapper objectMapper, Class<T> tClass) {
        try {
           return objectMapper.readValue(candidatesFilepath.toFile(), tClass);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static List<Double> readContextMatches(Path predictionsPath) throws IOException {
        return Files.lines(predictionsPath)
                .map(EntityLinkerUtils::getFirstNumberInTsvLine)
                .collect(Collectors.toList());
    }

    public static List<Pair<NamedEntity, List<CandidateWithContextMatch>>> mergeCandidatesAndContextMatches(
            List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions, List<Double> contextMatches) {
        AtomicInteger currentIdx = new AtomicInteger(0);
        return candidatesForMentions.stream().map(pair -> {
            List<WikiItemEntity> candidates = pair.getSecond();
            int fromIndex = currentIdx.get();
            int toIndex = currentIdx.addAndGet(candidates.size());
            List<Double> candidatesMatches = contextMatches.subList(fromIndex, toIndex);
            List<CandidateWithContextMatch> merged = merge(candidates, candidatesMatches);
            return Pair.of(pair.getFirst(), merged);
        }).collect(Collectors.toList());
    }

    public static void clearCandidatesContainingNoGood(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        candidatesForMentions.stream().filter(pair -> !pair.getSecond().isEmpty() && !containsGoodCandidate(pair.getSecond(), pair.getFirst()))
                .forEach(pair -> pair.getSecond().clear());
    }

    public static boolean containsGoodCandidate(List<WikiItemEntity> candidates, NamedEntity target) {
        return candidates.stream()
                .map(WikiItemEntity::getId)
                .collect(Collectors.toList())
                .contains(target.getEntityId());
    }

    public static void limitSearchResults(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions, int limit) {
        candidatesForMentions.forEach(pair -> {
            List<WikiItemEntity> limitedCandidates = pair.getSecond().stream()
                    .sorted((a, b) -> b.getNonNullMentionsCount() - a.getNonNullMentionsCount())
                    .limit(limit)
                    .collect(Collectors.toList());
            pair.getSecond().clear();
            pair.getSecond().addAll(limitedCandidates);
        });
    }

    public static void limitExtendedSearchResults(List<Pair<NamedEntity, List<CandidateWithContextMatch>>> candidatesForMentions, int limit) {
        candidatesForMentions.forEach(pair -> {
            List<CandidateWithContextMatch> limitedCandidates = pair.getSecond().stream()
                    .sorted((a, b) -> b.getWikiItemEntity().getNonNullMentionsCount() - a.getWikiItemEntity().getNonNullMentionsCount())
                    .limit(limit)
                    .collect(Collectors.toList());
            pair.getSecond().clear();
            pair.getSecond().addAll(limitedCandidates);
        });
    }

    private static List<CandidateWithContextMatch> merge(List<WikiItemEntity> candidates, List<Double> candidatesMatches) {
        return IntStream.range(0, candidates.size())
                .mapToObj(i -> new CandidateWithContextMatch(candidates.get(i), candidatesMatches.get(i)))
                .collect(Collectors.toList());
    }

    private static Double getFirstNumberInTsvLine(String line) {
        return Double.valueOf(line.split("\\t")[0]);
    }
}
