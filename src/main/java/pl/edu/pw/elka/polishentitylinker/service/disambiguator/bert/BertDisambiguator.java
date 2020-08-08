package pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.core.model.CandidateWithContextMatch;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.integration.gcp.BertIntegrationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static pl.edu.pw.elka.polishentitylinker.core.utils.EntityLinkerUtils.mergeCandidatesAndContextMatches;
import static pl.edu.pw.elka.polishentitylinker.core.utils.EntityLinkerUtils.readContextMatches;

@Primary
@Slf4j
@Service
@RequiredArgsConstructor
public class BertDisambiguator implements Disambiguator {

    private final BertDisambiguatorConfig config;
    private final BertService bertService;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS");

    @Override
    public WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates) {
        return null;
    }

    @Override
    public List<WikiItemEntity> chooseAll(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) {
        try {
            Path predictionsPath;
            if (config.isUseReadyPredictions()) {
                predictionsPath = Paths.get(config.getPredictionsPath());
            } else {
                Path uploadPath = prepareFileForBert(candidatesForMentions);
                String fileName = uploadPath.getFileName().toString();
                BertIntegrationUtils.uploadObject(config.getGcpProjectId(), config.getGsBucketName(),
                        getGsPath(config.getGsInputDir(), fileName), uploadPath);
                predictionsPath = waitForResultsAndDownloadWhereReady(fileName);
            }
            List<Double> contextMatches = readContextMatches(predictionsPath);
            List<Pair<NamedEntity, List<CandidateWithContextMatch>>> candidatesForMentionsWithMatches =
                    mergeCandidatesAndContextMatches(candidatesForMentions, contextMatches);
            return prepareResultList(candidatesForMentionsWithMatches);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<WikiItemEntity> prepareResultList(List<Pair<NamedEntity, List<CandidateWithContextMatch>>> candidatesForMentionsWithMatches) {
        List<WikiItemEntity> resultList = new ArrayList<>();
        candidatesForMentionsWithMatches.forEach(pair -> {
            List<CandidateWithContextMatch> candidates = pair.getSecond();
            int idxOfBestCandidate;
            if (config.isUsePopularity()) {
                idxOfBestCandidate = getMaxIdxFromList(getRatingsWithPopularity(candidates));
            } else {
                idxOfBestCandidate = getMaxIdxFromList(getStrictMatchesFromPredictions(candidates));
            }
            resultList.add(idxOfBestCandidate == -1 ? null : candidates.get(idxOfBestCandidate).getWikiItemEntity());
        });
        return resultList;
    }

    private List<Double> getRatingsWithPopularity(List<CandidateWithContextMatch> candidates) {
        int allCandidatesMentionsCount = candidates.stream().mapToInt(candidate -> getMentionsCount(candidate.getWikiItemEntity())).sum();
        List<Double> candidatesProbabilities = candidates.stream()
                .mapToDouble(candidate -> getMentionsCount(candidate.getWikiItemEntity()) / (double) allCandidatesMentionsCount)
                .boxed()
                .collect(Collectors.toList());

        return IntStream.range(0, candidates.size())
                .mapToDouble(i -> candidates.get(i).getContextMatch() + config.getPopularityRate() * candidatesProbabilities.get(i))
                .boxed()
                .collect(Collectors.toList());
    }

    private List<Double> getStrictMatchesFromPredictions(List<CandidateWithContextMatch> candidates) {
        return candidates.stream().map(CandidateWithContextMatch::getContextMatch).collect(Collectors.toList());
    }

    private Path prepareFileForBert(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        int candidatesForMentionsSize = candidatesForMentions.size();
        AtomicInteger processedSize = new AtomicInteger(0);
        candidatesForMentions.forEach(pair -> {
            NamedEntity namedEntity = pair.getFirst();
            List<WikiItemEntity> candidates = pair.getSecond();
            if (candidates.size() == 1) {
                stringBuffer.append(BertIntegrationUtils.prepeareEmptyExample());
            } else {
                candidates.forEach(candidate ->
                        stringBuffer.append(BertIntegrationUtils.prepareExampleForClassifier(namedEntity.getPageId(),
                                namedEntity, candidate, config.getArticlePartSize(), config.getArticlesDirectory()))
                );
            }
            log.info("{}/{} entities prepared for bert processing", processedSize.incrementAndGet(), candidatesForMentionsSize);
        });

        Path uploadPath = Paths.get(config.getLocalUploadDir(), String.format("candidates_%s.tsv", LocalDateTime.now().format(formatter)));
        Files.write(uploadPath, stringBuffer.toString().getBytes());
        log.info("File to upload created");
        return uploadPath;
    }

    private Path waitForResultsAndDownloadWhereReady(String filename) {
        Path localFilePath = Paths.get(config.getLocalDownloadDir(), getResultsFilename(filename));
        boolean bertProcessingFinished = false;
        while (!bertProcessingFinished) {
            boolean processedWithSuccess = bertService.exists(getGsPath(config.getGsSuccessDir(), filename));
            boolean processedWithError = bertService.exists(getGsPath(config.getGsErrorDir(), filename));

            if (processedWithSuccess) {
                String resultGsPath = getGsPath(config.getGsResultDir(), getResultsFilename(filename));
                if (!bertService.exists(resultGsPath)) {
                    throw new IllegalStateException("result file not exists");
                }
                log.info("File processed with success");
                bertProcessingFinished = true;
                bertService.download(resultGsPath, localFilePath);
            } else if (processedWithError) {
                log.error("File processed with error");
                throw new RuntimeException("File processed with error");
            } else {
                log.info("Waiting for result ...");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return localFilePath;
    }

    private String getGsPath(String directory, String filename) {
        return String.format("%s/%s", directory, filename);
    }

    private String getResultsFilename(String filename) {
        return String.format("%s_result.tsv", FilenameUtils.removeExtension(filename));
    }

    private int getMaxIdxFromList(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }
        Double maxVal = values.stream().reduce(Double::max).get();
        return values.indexOf(maxVal);
    }

    private Integer getMentionsCount(WikiItemEntity wikiItem) {
        return Optional.ofNullable(wikiItem.getMentionsCount()).orElse(0);
    }
}
