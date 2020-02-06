package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
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

@Getter
public class TaggedTextIterator {

    private static final String NO_NAMED_ENTITY_MARK = "_";

    private List<TokenizedWord> loaded;
    private List<NamedEntity> namedEntities;

    public void processFile(String path) {
        Path path1 = Paths.get(path);

        try {
            loaded = Files.lines(path1).map(this::parseLine).collect(Collectors.toList());
            processLoaded();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TokenizedWord parseLine(String line) {
       return TsvLineParser.parseTokenizedWord(line);
    }

    private void processLoaded() {
        namedEntities = new ArrayList<>();
        List<Integer> namedEntitiesIdxs = IntStream.range(0, loaded.size())
                .filter(i -> isNamedEntity(loaded.get(i)))
                .boxed()
                .collect(Collectors.toList());

        if (!namedEntitiesIdxs.isEmpty()) {
            List<TokenizedWord> buffer = new ArrayList<>();
            String lastId = loaded.get(namedEntitiesIdxs.get(0)).getEntityId();

            for (Integer idx : namedEntitiesIdxs) {
                TokenizedWord tokenizedWord = loaded.get(idx);
                if (tokenizedWord.getEntityId().equals(lastId)) {
                    buffer.add(tokenizedWord);
                } else {
                    addFromBuffer(buffer);
                    buffer.add(tokenizedWord);
                    lastId = tokenizedWord.getEntityId();
                }
            }
            addFromBuffer(buffer);
        }

    }

    private void addFromBuffer(List<TokenizedWord> buffer) {
        NamedEntity namedEntity = new NamedEntity();
        List<TokenizedWord> entitySpan = new ArrayList<>(buffer);
        namedEntity.setEntitySpan(entitySpan);
        namedEntities.add(namedEntity);
        buffer.clear();
    }

    private boolean isNamedEntity(TokenizedWord tokenizedWord) {
        return tokenizedWord != null && !NO_NAMED_ENTITY_MARK.equals(tokenizedWord.getEntityId());
    }


}
