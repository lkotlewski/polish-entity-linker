package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entity.AliasEntity;
import pl.edu.pw.elka.polishentitylinker.entity.AliasLemmatizedEntity;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedExtendedWord;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.CorpusProcessorConfig;
import pl.edu.pw.elka.polishentitylinker.repository.AliasLemmatizedRepository;
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

    public static final String SURNAME_AND_NAME_WITH_COMMA_AND_POLISH_SIGNS_PATTERN = "[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+,\\s[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+";
    private final CorpusProcessorConfig config;
    private final BatchProcessingConfig batchProcessingConfig;
    private final WikiItemRepository wikiItemRepository;
    private final AliasRepository aliasRepository;
    private final AliasLemmatizedRepository aliasLemmatizedRepository;
    private BufferedBatchProcessor<AliasEntity> aliasesSaver;
    private BufferedBatchProcessor<AliasLemmatizedEntity> aliasesLemmatizedSaver;

    private Map<String, Integer> entitiesMentions = new HashMap<>();
    private Map<String, Set<String>> entitiesAliases = new HashMap<>();
    private Map<String, Set<String>> entitiesLemmatizedAliases = new HashMap<>();
    private StringBuilder aliasStringBuilder = new StringBuilder();
    private StringBuilder aliasLemmatizedStringBuilder = new StringBuilder();
    private String lastEntityId;

    private BufferedBatchProcessor<WikiItemEntity> articlesLengthSaver;
    private Map<Integer, Integer> articlesLength = new HashMap<>();
    private Integer lastDocId;
    private Integer processedCount = 0;

    @Override
    public void processFile() {
        aliasesSaver = new BufferedBatchProcessor<>(aliasRepository::saveAll, batchProcessingConfig.getSize());
        aliasesLemmatizedSaver = new BufferedBatchProcessor<>(aliasLemmatizedRepository::saveAll, batchProcessingConfig.getSize());
        articlesLengthSaver = new BufferedBatchProcessor<>(wikiItemRepository::saveAll, batchProcessingConfig.getSize());
        processLineByLine(config.getFilepath());
        if (config.isCountMentions()) {
            saveCountResults();
        }
        if (config.isExtractAliases()) {
            saveAliases();
        }
        if (config.isExtractLemmatizedAliases()) {
            saveLemmatizedAliases();
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
        if (config.isCountMentions() || config.isExtractAliases() || config.isExtractLemmatizedAliases()) {
            String entityId = tokenizedWord.getEntityId();
            if (isNamedEntity(lastEntityId) && !isContinuation(entityId)) {
                handleEntitySpanEnd();
            }
            if (isNamedEntity(entityId)) {
                if (isContinuation(entityId)) {
                    appendSpaceIfPreceded(tokenizedWord);
                }
                aliasStringBuilder.append(tokenizedWord.getToken());
                if (tokenizedWord instanceof TokenizedExtendedWord) {
                    aliasLemmatizedStringBuilder.append(((TokenizedExtendedWord) tokenizedWord).getLemma());
                }
            }
            lastEntityId = entityId;
        }
    }

    private void processDocId(TokenizedWord tokenizedWord) {
        if (config.isEvalArticlesLength() || config.isLogProcessedDocsNumber()) {
            Integer docId = tokenizedWord.getDocId();
            if (config.isEvalArticlesLength()) {
                if (articlesLength.containsKey(docId)) {
                    articlesLength.put(docId, articlesLength.get(docId) + 1);
                } else {
                    articlesLength.put(docId, 1);
                }
            }
            if (config.isLogProcessedDocsNumber()) {
                if (!docId.equals(lastDocId)) {
                    processedCount++;
                    log.info("processed {} docs", processedCount);
                    lastDocId = docId;
                }
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
            aliasStringBuilder.append(" ");
            aliasLemmatizedStringBuilder.append(" ");
        }
    }

    private void handleEntitySpanEnd() {
        if (config.isCountMentions()) {
            incrementMentionsCount(lastEntityId);
        }
        if (config.isExtractAliases()) {
            addAlias(lastEntityId, aliasStringBuilder.toString());
        }
        if (config.isExtractLemmatizedAliases()) {
            addLemmatizedAlias(lastEntityId, aliasLemmatizedStringBuilder.toString());
        }
        aliasStringBuilder.setLength(0);
        aliasLemmatizedStringBuilder.setLength(0);
    }

    private void incrementMentionsCount(String entityId) {
        if (entitiesMentions.containsKey(entityId)) {
            entitiesMentions.put(entityId, entitiesMentions.get(entityId) + 1);
        } else {
            entitiesMentions.put(entityId, 1);
        }
    }

    private void addAlias(String entityId, String alias) {
        addAliasToSelectedAliasesList(entityId, alias, entitiesAliases);
    }

    private void addLemmatizedAlias(String entityId, String alias) {
        addAliasToSelectedAliasesList(entityId, alias, entitiesLemmatizedAliases);
    }

    private void addAliasToSelectedAliasesList(String entityId, String alias, Map<String, Set<String>> entitiesAliases) {
        Set<String> aliases;
        if (entitiesAliases.containsKey(entityId)) {
            aliases = entitiesAliases.get(entityId);
        } else {
            aliases = new HashSet<>();
        }
        aliases.add(alias);
        if (alias.matches(SURNAME_AND_NAME_WITH_COMMA_AND_POLISH_SIGNS_PATTERN)) {
            aliases.add(alias.split(",")[0]);
        }
        entitiesAliases.put(entityId, aliases);
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

    private void saveLemmatizedAliases() {
        log.info("saving lemmatized aliases");
        entitiesLemmatizedAliases.forEach((k, aliases) -> wikiItemRepository.findById(k).ifPresent(wikiItemEntity ->
                aliases.forEach(alias -> {
                    if (PageType.REGULAR_ARTICLE.equals(wikiItemEntity.getPageType()) && alias.length() < 255) {
                        AliasLemmatizedEntity aliasLemmatizedEntity = new AliasLemmatizedEntity();
                        aliasLemmatizedEntity.setLabel(alias);
                        aliasLemmatizedEntity.setTarget(wikiItemEntity);
                        aliasesLemmatizedSaver.process(aliasLemmatizedEntity);
                    }
                })));
        aliasesLemmatizedSaver.processRest();
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
