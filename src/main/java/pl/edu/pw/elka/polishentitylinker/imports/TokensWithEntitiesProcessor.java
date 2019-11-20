package pl.edu.pw.elka.polishentitylinker.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.AliasEntity;
import pl.edu.pw.elka.polishentitylinker.imports.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.repository.AliasRepository;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokensWithEntitiesProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemRepository wikiItemRepository;
    private final AliasRepository aliasRepository;
    private BufferedBatchProcessor<AliasEntity> aliasesSaver;

    private Map<String, Integer> entitiesMentions = new HashMap<>();
    private Map<String, Set<String>> entitiesAliases = new HashMap<>();
    private StringBuilder stringBuilder = new StringBuilder();
    private String lastEntityId;

    @Override
    public void parseFile() {
        aliasesSaver = new BufferedBatchProcessor<>(aliasRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        parseFile(itemsParserConfig.getTokensWithEntitiesFilepath());
        saveCountResults();
        saveAliases();
    }

    @Override
    void processLine(String line) {
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null) {
            String entityId = tokenizedWord.getEntityId();
            if (isNamedEntity(entityId)) {
                if (isContinuation(entityId)) {
                    if (tokenizedWord.isPrecedingSpace()) {
                        stringBuilder.append(" ");
                    }
                    stringBuilder.append(tokenizedWord.getToken());
                } else {
                    stringBuilder.append(tokenizedWord.getToken());
                    incrementMentionsCount(entityId);
                }
            } else {
                if (stringBuilder.length() != 0) {
                    addAlias(lastEntityId, stringBuilder.toString());
                    stringBuilder.setLength(0);
                }
            }
            lastEntityId = entityId;
        }
    }

    private void incrementMentionsCount(String entityId) {
        if (entitiesMentions.containsKey(entityId)) {
            entitiesMentions.put(entityId, entitiesMentions.get(entityId) + 1);
            log.info(entityId);
        } else {
            entitiesMentions.put(entityId, 1);
            log.info(entityId);
        }
    }

    private void addAlias(String entityId, String alias) {
        if (entitiesAliases.containsKey(entityId)) {
            Set<String> aliases = entitiesAliases.get(entityId);
            aliases.add(alias);
            log.info(entityId);
        } else {
            Set<String> aliases = new HashSet<>();
            aliases.add(alias);
            entitiesAliases.put(entityId, aliases);
            log.info(entityId);
        }
    }


    private boolean isNamedEntity(String entityId) {
        return !"_".equals(entityId);
    }

    private boolean isContinuation(String entityId) {
        return entityId.equals(lastEntityId);
    }

    private void saveCountResults() {
        log.info("saving counts");
        log.info(String.valueOf(entitiesMentions.size()));
        entitiesMentions.forEach((k, v) -> wikiItemRepository.findById(k).ifPresent(wikiItemEntity -> {
            wikiItemEntity.setMentionsCount(v);
            wikiItemRepository.save(wikiItemEntity);
            log.info(k + " counts saved");
        }));
    }

    private void saveAliases() {
        log.info("saving aliases");
        log.info(String.valueOf(entitiesMentions.size()));
        entitiesAliases.forEach((k, aliases) -> wikiItemRepository.findById(k).ifPresent(wikiItemEntity -> {

            aliases.forEach(alias -> {
                if (PageType.REGULAR_ARTICLE.equals(wikiItemEntity.getPageType()) && alias.length() < 255) {
                    AliasEntity aliasEntity = new AliasEntity();
                    aliasEntity.setLabel(alias);
                    aliasEntity.setTarget(wikiItemEntity);
                    aliasesSaver.process(aliasEntity);
                }
            });
        }));
        aliasesSaver.processRest();
    }


}
