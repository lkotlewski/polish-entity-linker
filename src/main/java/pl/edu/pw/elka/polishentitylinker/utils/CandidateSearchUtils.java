package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import one.util.streamex.StreamEx;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CandidateSearchUtils {

    public static List<WikiItemEntity> getDistinctRegularArticles(List<WikiItemEntity> candidates) {
        return StreamEx.of(candidates)
                .distinct(WikiItemEntity::getId)
                .filter(wikiItemEntity -> PageType.REGULAR_ARTICLE.equals(wikiItemEntity.getPageType()))
                .collect(Collectors.toList());
    }
}
