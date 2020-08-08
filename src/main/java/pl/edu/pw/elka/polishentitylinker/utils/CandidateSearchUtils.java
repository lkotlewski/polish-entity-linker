package pl.edu.pw.elka.polishentitylinker.utils;

import one.util.streamex.StreamEx;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;

import java.util.List;
import java.util.stream.Collectors;

public class CandidateSearchUtils {

    public static List<WikiItemEntity> getDistinctRegularArticles(List<WikiItemEntity> candidates) {
        return StreamEx.of(candidates)
                .distinct(WikiItemEntity::getId)
                .filter(wikiItemEntity -> PageType.REGULAR_ARTICLE.equals(wikiItemEntity.getPageType()))
                .collect(Collectors.toList());
    }
}
