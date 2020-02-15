package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Getter
public class TaggedTextIterator {

    private static final String NO_NAMED_ENTITY_MARK = "_";

    private final int windowSize = 50;
    private List<TokenizedWord> loaded;
    private List<NamedEntity> namedEntities;
    private int currentEntityStartIdx;

    public void processFile(String path) {
        Path path1 = Paths.get(path);

        try {
            loaded = Files.lines(path1).map(this::parseLine).collect(Collectors.toList());
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
        List<Integer> namedEntitiesIdxs = getNamedEntitiesIdxs();

        if (!namedEntitiesIdxs.isEmpty()) {
            List<TokenizedWord> buffer = new ArrayList<>();
            String lastId = null;

            for (Integer idx : namedEntitiesIdxs) {
                TokenizedWord tokenizedWord = loaded.get(idx);
                if (!tokenizedWord.getEntityId().equals(lastId)) {
                    if (lastId != null) {
                        savePreviousAndClearBuffer(buffer);
                    }
                    lastId = tokenizedWord.getEntityId();
                    currentEntityStartIdx = idx;
                }
                buffer.add(tokenizedWord);
            }
            savePreviousAndClearBuffer(buffer);
        }

    }

    private List<Integer> getNamedEntitiesIdxs() {
        return IntStream.range(0, loaded.size())
                .filter(i -> isNamedEntity(loaded.get(i)))
                .boxed()
                .collect(Collectors.toList());
    }

    private void savePreviousAndClearBuffer(List<TokenizedWord> buffer) {
        NamedEntity namedEntity = new NamedEntity();
        List<TokenizedWord> entitySpan = new ArrayList<>(buffer);
        namedEntity.setEntitySpan(entitySpan);
        namedEntities.add(namedEntity);
        namedEntity.setContext(getContext());
        buffer.clear();
    }

    private boolean isNamedEntity(TokenizedWord tokenizedWord) {
        return tokenizedWord != null && !NO_NAMED_ENTITY_MARK.equals(tokenizedWord.getEntityId());
    }

    private List<TokenizedWord> getContext() {
        int halfWindowSize = windowSize / 2;
        int lastValidIdx = loaded.size() - 1;
        if (windowSize > loaded.size()) {
            return loaded;
        }
        if (currentEntityStartIdx + halfWindowSize > lastValidIdx) {
            return loaded.subList(lastValidIdx - windowSize + 1, lastValidIdx + 1);
        }
        if (currentEntityStartIdx - halfWindowSize < 0) {
            return loaded.subList(0, windowSize);
        }

        return loaded.subList(currentEntityStartIdx - halfWindowSize, currentEntityStartIdx + halfWindowSize);

    }

}
