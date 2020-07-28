package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Getter
public class TaggedTextIterator {

    private static final String NO_NAMED_ENTITY_MARK = "_";

    private final int windowSize = 50;
    private Map<Integer, List<TokenizedWord>> loadedArticles;
    private List<NamedEntity> namedEntities;
    private int currentEntityStartIdx;

    public void processFile(String filepath) {
        processFile(Paths.get(filepath));
    }

    public void processFile(Path path) {
        try {
            loadedArticles =
                    Files.lines(path)
                            .map(this::parseLine)
                            .filter(Objects::nonNull)
                            .collect(groupingBy(TokenizedWord::getDocId));
            processLoaded();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private TokenizedWord parseLine(String line) {
        return TsvLineParser.parseTokenizedWord(line);
    }

    private void processLoaded() {
        namedEntities = new ArrayList<>();

        loadedArticles.forEach((docId, article) -> {
            List<Integer> namedEntitiesIdxs = getNamedEntitiesIdxs(article);
            if (!namedEntitiesIdxs.isEmpty()) {
                List<TokenizedWord> buffer = new ArrayList<>();
                String lastId = null;
                Integer lastIdx = -100;

                for (Integer idx : namedEntitiesIdxs) {
                    TokenizedWord tokenizedWord = article.get(idx);
                    boolean previousHasDifferentId = !tokenizedWord.getEntityId().equals(lastId);
                    boolean previousWasFurtherThanOneWord = !idx.equals(lastIdx  +1);
                    boolean newSpan = previousHasDifferentId || previousWasFurtherThanOneWord;
                    if (newSpan) {
                        savePreviousAndClearBuffer(buffer, article);
                        currentEntityStartIdx = idx;
                    }
                    buffer.add(tokenizedWord);
                    lastId = tokenizedWord.getEntityId();
                    lastIdx = idx;
                }
                savePreviousAndClearBuffer(buffer, article);
            }
        });
    }

    private List<Integer> getNamedEntitiesIdxs(List<TokenizedWord> article) {
        return IntStream.range(0, article.size())
                .filter(i -> isNamedEntity(article.get(i)))
                .boxed()
                .collect(Collectors.toList());
    }

    private void savePreviousAndClearBuffer(List<TokenizedWord> buffer, List<TokenizedWord> article) {
        if(!buffer.isEmpty()) {
            NamedEntity namedEntity = new NamedEntity();
            List<TokenizedWord> entitySpan = new ArrayList<>(buffer);
            namedEntity.setEntitySpan(entitySpan);
            namedEntities.add(namedEntity);
            namedEntity.setContext(getContext(article));
            buffer.clear();
        }
    }

    private boolean isNamedEntity(TokenizedWord tokenizedWord) {
        return tokenizedWord != null && !NO_NAMED_ENTITY_MARK.equals(tokenizedWord.getEntityId());
    }

    private List<TokenizedWord> getContext(List<TokenizedWord> article) {
        int halfWindowSize = windowSize / 2;
        int lastValidIdx = article.size() - 1;
        if (windowSize > article.size()) {
            return article;
        }
        if (currentEntityStartIdx + halfWindowSize > lastValidIdx) {
            return article.subList(lastValidIdx - windowSize + 1, lastValidIdx + 1);
        }
        if (currentEntityStartIdx - halfWindowSize < 0) {
            return article.subList(0, windowSize);
        }

        return article.subList(currentEntityStartIdx - halfWindowSize, currentEntityStartIdx + halfWindowSize);

    }

}
