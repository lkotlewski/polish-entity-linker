package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BertIntegrationUtils {

    private static final String LINE_PATTERN = "%s\t%d\t%s\t%d\t%s\t%s\t%b\n";

    public static String prepareExampleForClassifier(Integer contextPageId, NamedEntity targetNamedEntity,
            WikiItemEntity candidateWikiItem, int articlePartSize, String articlesDirectory) {
        return prepareDatasetLine(
                targetNamedEntity.toOriginalForm(),
                contextPageId,
                targetNamedEntity.getEntityId(),
                candidateWikiItem.getPageId(),
                targetNamedEntity.getContextAsString(),
                getArticleBeginningByPageId(candidateWikiItem.getPageId(), articlePartSize, articlesDirectory),
                targetNamedEntity.getEntityId().equals(candidateWikiItem.getId())
        );
    }

    private static String prepareDatasetLine(String originalForm, Integer contextArticleId, String targetWikiItemId,
            Integer comparedArticleId, String context, String compared, boolean positiveExample) {
        return String.format(LINE_PATTERN, originalForm, contextArticleId, targetWikiItemId, comparedArticleId,
                context, compared, positiveExample);
    }

    private static String getArticleBeginningByPageId(Integer pageId, int articlePartSize, String articlesDirectory) {
        Path path = getArticlePath(pageId, articlesDirectory);
        List<TokenizedWord> collect = new ArrayList<>();
        try {
            collect = Files.lines(path)
                    .map(TsvLineParser::parseTokenizedWord)
                    .filter(Objects::nonNull)
                    .limit(articlePartSize)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return TokenizedTextUtils.spanToOriginalForm(collect);
    }

    private static Path getArticlePath(Integer pageId, String articlesDirectory) {
        return Paths.get(articlesDirectory, String.format("%d.tsv", pageId));
    }

}
