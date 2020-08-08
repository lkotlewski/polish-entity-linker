package pl.edu.pw.elka.polishentitylinker.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidatesForMentions {

    public CandidatesForMentions(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        targetNamedEntities = candidatesForMentions.stream().map(Pair::getFirst).collect(Collectors.toList());
        candidates = candidatesForMentions.stream().map(Pair::getSecond).collect(Collectors.toList());
    }

    List<NamedEntity> targetNamedEntities;
    List<List<WikiItemEntity>> candidates;

    @JsonIgnore
    public List<Pair<NamedEntity, List<WikiItemEntity>>> getPaired() {
        List<Pair<NamedEntity, List<WikiItemEntity>>> paired = new ArrayList<>();
        IntStream.range(0, candidates.size()).forEach(i -> paired.add(Pair.of(targetNamedEntities.get(i), candidates.get(i))));
        return paired;
    }
}
