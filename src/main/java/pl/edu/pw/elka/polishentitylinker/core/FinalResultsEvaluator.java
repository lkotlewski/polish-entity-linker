package pl.edu.pw.elka.polishentitylinker.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.config.FinalResultsEvaluatorConfig;
import pl.edu.pw.elka.polishentitylinker.core.model.CandidateWithContextMatch;
import pl.edu.pw.elka.polishentitylinker.core.model.result.DisambiguatorResults;
import pl.edu.pw.elka.polishentitylinker.core.model.result.ResultForConfiguration;
import pl.edu.pw.elka.polishentitylinker.core.model.result.SearcherResults;
import pl.edu.pw.elka.polishentitylinker.core.model.result.WholeSystemResults;
import pl.edu.pw.elka.polishentitylinker.core.utils.EntityLinkerUtils;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert.BertDisambiguator;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert.BertDisambiguatorConfig;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert.BertService;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import static pl.edu.pw.elka.polishentitylinker.core.utils.EntityLinkerUtils.*;
import static pl.edu.pw.elka.polishentitylinker.core.utils.RaportPreparator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalResultsEvaluator {

    private final FinalResultsEvaluatorConfig config;
    private final ObjectMapper objectMapper;

    private List<Float> popularityRates = Arrays.asList(0F);
    private List<Integer> candidatesLimits = Arrays.asList(10);

    public void evaluateResults() {

        List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions =
                EntityLinkerUtils.readSearcherResults(Paths.get(config.getCandidatesFilepath()), objectMapper);
        List<Double> contextMatches = getContextMatches();
        List<NamedEntity> referenceEntities = candidatesForMentions.stream().map(Pair::getFirst).collect(Collectors.toList());
        List<ResultForConfiguration> resultsForConfigurations = new ArrayList<>();

        popularityRates.forEach(popularityRate -> {
            candidatesLimits.stream().forEach(limit -> {
                log.info("\n\n###########################");
                log.info("candidates limit: {}, popularity rate: {}", limit, popularityRate);
                log.info("###########################");
                BertDisambiguator bertDisambiguator = getConfiguredDisambiguator(popularityRate);

                List<Pair<NamedEntity, List<WikiItemEntity>>> copiedCandidates = withCandidatesDeepCopy(candidatesForMentions);
                List<Pair<NamedEntity, List<WikiItemEntity>>> copiedForEvaluation = withCandidatesDeepCopy(candidatesForMentions);
                List<Double> copiedMatches = deepCopyList(contextMatches);

                clearCandidatesContainingNoGood(copiedCandidates);
                int allCandidatesSum = copiedCandidates.stream().mapToInt(c -> c.getSecond().size()).sum();
                if(allCandidatesSum != contextMatches.size()) {
                    throw new IllegalStateException("Predicitions does not match to candidates");
                }
                List<Pair<NamedEntity, List<CandidateWithContextMatch>>> merged =
                        mergeCandidatesAndContextMatches(copiedCandidates, copiedMatches);
                limitExtendedSearchResults(merged, limit);

                List<WikiItemEntity> chosenEntities = bertDisambiguator.prepareResultList(merged);
                if (chosenEntities.size() != referenceEntities.size()) {
                    throw new IllegalStateException("Result list size is different from size of entities to disambiguate list");
                }

                EntityLinkerUtils.limitSearchResults(copiedForEvaluation, limit);
                SearcherResults searcherResults = evaluateSearcherResultsParams(copiedForEvaluation);
                DisambiguatorResults disambiguatorResults = evaluateDisambiguatorParams(chosenEntities, copiedForEvaluation);
                WholeSystemResults wholeSystemResults = evaluateOverallParams(chosenEntities, referenceEntities);
                ResultForConfiguration result = ResultForConfiguration.builder()
                        .popularityRate(popularityRate)
                        .candidatesLimit(limit)
                        .searcherResults(searcherResults)
                        .disambiguatorResults(disambiguatorResults)
                        .wholeSystemResults(wholeSystemResults)
                        .build();
                resultsForConfigurations.add(result);
            });
        });

        OptionalDouble max = resultsForConfigurations.stream().mapToDouble((a) -> a.getWholeSystemResults().getAccuracy()).max();
        log.info("###########################\n\n");
        log.info(String.format("Best accuracy: %.4f", max.getAsDouble()));
        saveToFile(Paths.get(config.getEvaluationsFilepath()), resultsForConfigurations, objectMapper);
    }

    private BertDisambiguator getConfiguredDisambiguator(Float popularityRate) {
        BertDisambiguatorConfig bertDisambiguatorConfig = new BertDisambiguatorConfig();
        bertDisambiguatorConfig.setUsePopularity(true);
        bertDisambiguatorConfig.setPopularityRate(popularityRate);
        return new BertDisambiguator(bertDisambiguatorConfig,
                new BertService(bertDisambiguatorConfig));
    }

    private List<Double> getContextMatches() {
        try {
            return EntityLinkerUtils.readContextMatches(Paths.get(config.getPredictionsFilepath()));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
