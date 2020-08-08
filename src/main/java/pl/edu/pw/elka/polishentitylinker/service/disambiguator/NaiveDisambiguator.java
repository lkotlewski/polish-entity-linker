package pl.edu.pw.elka.polishentitylinker.service.disambiguator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaiveDisambiguator implements Disambiguator {

    @Override
    public WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates) {
        return candidates
                .stream()
                .reduce((a, b) -> a.getNonNullMentionsCount() > b.getNonNullMentionsCount() ? a : b)
                .orElse(null);
    }

    @Override
    public List<WikiItemEntity> chooseAll(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        List<WikiItemEntity> results = new ArrayList<>();
        int candidatesForMentionsSize = candidatesForMentions.size();
        AtomicInteger processedSize = new AtomicInteger(0);
        candidatesForMentions.forEach(pair -> {
            NamedEntity namedEntity = pair.getFirst();
            List<WikiItemEntity> candidates = pair.getSecond();
            results.add(choose(namedEntity, candidates));
            log.info("{}/{} entities disambigutated", processedSize.incrementAndGet(), candidatesForMentionsSize);
        });
        return results;
    }
}
