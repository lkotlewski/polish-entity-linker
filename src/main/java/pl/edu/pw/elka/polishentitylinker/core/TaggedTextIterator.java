package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
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

    private List<TokenizedExtendedWord> loaded;
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

    private TokenizedExtendedWord parseLine(String line) {
       return TsvLineParser.parseTokenizedExtendedWord(line);
    }

    private void processLoaded() {
        namedEntities = new ArrayList<>();
        List<Integer> namedEntitiesIdxs = IntStream.range(0, loaded.size())
                .filter(i -> isNamedEntity(loaded.get(i)))
                .boxed()
                .collect(Collectors.toList());

        if (!namedEntitiesIdxs.isEmpty()) {
            List<TokenizedExtendedWord> buffer = new ArrayList<>();
            String lastId = loaded.get(namedEntitiesIdxs.get(0)).getEntityId();

            for (Integer idx : namedEntitiesIdxs) {
                TokenizedExtendedWord tokenizedExtendedWord = loaded.get(idx);
                if (tokenizedExtendedWord.getEntityId().equals(lastId)) {
                    buffer.add(tokenizedExtendedWord);
                } else {
                    addFromBuffer(buffer);
                    buffer.add(tokenizedExtendedWord);
                    lastId = tokenizedExtendedWord.getEntityId();
                }
            }
            addFromBuffer(buffer);
        }

    }

    private void addFromBuffer(List<TokenizedExtendedWord> buffer) {
        NamedEntity namedEntity = new NamedEntity();
        List<TokenizedExtendedWord> entitySpan = new ArrayList<>(buffer);
        namedEntity.setEntitySpan(entitySpan);
        namedEntities.add(namedEntity);
        buffer.clear();
    }

    private boolean isNamedEntity(TokenizedExtendedWord tokenizedExtendedWord) {
        return tokenizedExtendedWord != null && !NO_NAMED_ENTITY_MARK.equals(tokenizedExtendedWord.getEntityId());
    }


}
