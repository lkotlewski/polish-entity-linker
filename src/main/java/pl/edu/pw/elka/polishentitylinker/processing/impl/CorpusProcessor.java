package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.AliasEntity;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.CorpusProcessorConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
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
public class CorpusProcessor extends LineFileProcessor {

    private final CorpusProcessorConfig config;
    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemRepository wikiItemRepository;
    private final AliasRepository aliasRepository;
    private BufferedBatchProcessor<AliasEntity> aliasesSaver;

    private Map<String, Integer> entitiesMentions = new HashMap<>();
    private Map<String, Set<String>> entitiesAliases = new HashMap<>();
    private StringBuilder stringBuilder = new StringBuilder();
    private String lastEntityId;

    private BufferedBatchProcessor<WikiItemEntity> articlesLengthSaver;
    private Map<Integer, Integer> articlesLength = new HashMap<>();
    private Integer lastDocId;

    @Override
    public void processFile() {
        aliasesSaver = new BufferedBatchProcessor<>(aliasRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        articlesLengthSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        processLineByLine(config.getFilepath());
        if (config.isCountMentions()) {
            saveCountResults();
        }
        if (config.isExtractAliases()) {
            saveAliases();
        }
        if (config.isEvalArticlesLength()) {
            saveArticlesLengthResults();
        }
    }

    @Override
    protected void processLine(String line) {
        TokenizedWord tokenizedWord = TsvLineParser.parseTokenizedWord(line);
        if (tokenizedWord != null) {
            processEntityId(tokenizedWord);
            processDocId(tokenizedWord);
        }
    }

    private void processEntityId(TokenizedWord tokenizedWord) {
        if(config.isCountMentions() || config.isExtractAliases()) {
            String entityId = tokenizedWord.getEntityId();
            if (isNamedEntity(lastEntityId) && !isContinuation(entityId)) {
                handleEntitySpanEnd();
            }
            if (isNamedEntity(entityId)) {
                if (isContinuation(entityId)) {
                    appendSpaceIfPreceded(tokenizedWord);
                }
                stringBuilder.append(tokenizedWord.getToken());
            }
            lastEntityId = entityId;
        }
    }

    private void processDocId(TokenizedWord tokenizedWord) {
        if (config.isEvalArticlesLength()) {
            Integer docId = tokenizedWord.getDocId();
            if (articlesLength.containsKey(docId)) {
                articlesLength.put(docId, articlesLength.get(docId) + 1);
            } else {
                articlesLength.put(docId, 1);
            }
            if (!docId.equals(lastDocId)) {
                log.info("processing {}", docId);
                lastDocId = docId;
            }
        }
    }

    private boolean isNamedEntity(String entityId) {
        return entityId != null && !"_".equals(entityId);
    }

    private boolean isContinuation(String entityId) {
        return entityId.equals(lastEntityId);
    }


    private void appendSpaceIfPreceded(TokenizedWord tokenizedWord) {
        if (tokenizedWord.isPrecedingSpace()) {
            stringBuilder.append(" ");
        }
    }

    private void handleEntitySpanEnd() {
        if (config.isCountMentions()) {
            incrementMentionsCount(lastEntityId);
        }
        if (config.isExtractAliases()) {
            addAlias(lastEntityId, stringBuilder.toString());
        }
        stringBuilder.setLength(0);
    }

    private void incrementMentionsCount(String entityId) {
        if (entitiesMentions.containsKey(entityId)) {
            entitiesMentions.put(entityId, entitiesMentions.get(entityId) + 1);
        } else {
            entitiesMentions.put(entityId, 1);
        }
        log.info("{} mentions count incremented", entityId);
    }

    private void addAlias(String entityId, String alias) {
        Set<String> aliases;
        if (entitiesAliases.containsKey(entityId)) {
            aliases = entitiesAliases.get(entityId);
        } else {
            aliases = new HashSet<>();
        }
        aliases.add(alias);
        entitiesAliases.put(entityId, aliases);
        log.info("{} alias added", entityId);
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
        entitiesAliases.forEach((k, aliases) -> wikiItemRepository.findById(k).ifPresent(wikiItemEntity ->
                aliases.forEach(alias -> {
                    if (PageType.REGULAR_ARTICLE.equals(wikiItemEntity.getPageType()) && alias.length() < 255) {
                        AliasEntity aliasEntity = new AliasEntity();
                        aliasEntity.setLabel(alias);
                        aliasEntity.setTarget(wikiItemEntity);
                        aliasesSaver.process(aliasEntity);
                    }
                })));
        aliasesSaver.processRest();
    }

    private void saveArticlesLengthResults() {
        log.info("saving articles lengths");
        articlesLength.forEach((docId, articleLength) -> {
            log.info(docId.toString());
            wikiItemRepository.findByPageId(docId).ifPresent(wikiItemEntity -> {
                wikiItemEntity.setArticleLength(articleLength);
                articlesLengthSaver.process(wikiItemEntity);
            });
        });
        articlesLengthSaver.processRest();
    }


}
