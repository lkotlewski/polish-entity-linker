package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.TaggedWord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class TaggedTextIterator {

    private static final String NO_NAMED_ENTITY_MARK = "_";

    private List<TaggedWord> loaded;
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

    private TaggedWord parseLine(String line) {
        List<String> parts = Arrays.asList(line.split("\\s"));
        if (parts.size() == 7) {
            return TaggedWord.builder()
                    .docId(Integer.parseInt(parts.get(0)))
                    .token(parts.get(1))
                    .lemma(parts.get(2))
                    .precedingSpace("1".equals(parts.get(3)))
                    .morphosyntacticTags(parts.get(4))
                    .linkTitle(parts.get(5))
                    .entityId(parts.get(6))
                    .build();
        }
        return null;
    }

    private void processLoaded() {
        namedEntities = new ArrayList<>();
        List<Integer> namedEntitiesIdxs = IntStream.range(0, loaded.size())
                .filter(i -> isNamedEntity(loaded.get(i)))
                .boxed()
                .collect(Collectors.toList());

        if (!namedEntitiesIdxs.isEmpty()) {
            List<TaggedWord> buffer = new ArrayList<>();
            String lastId = loaded.get(namedEntitiesIdxs.get(0)).getEntityId();

            for (Integer idx : namedEntitiesIdxs) {
                TaggedWord taggedWord = loaded.get(idx);
                if (taggedWord.getEntityId().equals(lastId)) {
                    buffer.add(taggedWord);
                } else {
                    addFromBuffer(buffer);
                    buffer.add(taggedWord);
                    lastId = taggedWord.getEntityId();
                }
            }
            addFromBuffer(buffer);
        }

    }

    private void addFromBuffer(List<TaggedWord> buffer) {
        NamedEntity namedEntity = new NamedEntity();
        List<TaggedWord> entitySpan = new ArrayList<>(buffer);
        namedEntity.setEntitySpan(entitySpan);
        namedEntities.add(namedEntity);
        buffer.clear();
    }

    private boolean isNamedEntity(TaggedWord taggedWord) {
        return taggedWord != null && !NO_NAMED_ENTITY_MARK.equals(taggedWord.getEntityId());
    }


}
