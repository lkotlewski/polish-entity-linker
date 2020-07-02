package pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.service.disambiguator.Disambiguator;
import pl.edu.pw.elka.polishentitylinker.utils.BertIntegrationUtils;

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
        List<WikiItemEntity> resultList = new ArrayList<>();
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
            List<Double> contextMatches = Files.lines(predictionsPath)
                    .map(this::getFirstNumberInTsvLine)
                    .collect(Collectors.toList());
            AtomicInteger currentIdx = new AtomicInteger(0);
            candidatesForMentions.forEach(pair -> {
                List<WikiItemEntity> candidates = pair.getSecond();
                int fromIndex = currentIdx.get();
                int toIndex = currentIdx.addAndGet(candidates.size());
                List<Double> candidatesMatches = contextMatches.subList(fromIndex, toIndex);
                int idxOfBestCandidate;
                if (config.isUsePopularity()) {
                    int allCandidatesMentionsCount = candidates.stream().mapToInt(this::getMentionsCount).sum();
                    List<Double> candidatesProbabilities = candidates.stream()
                            .mapToDouble(wikiItemEntity -> getMentionsCount(wikiItemEntity) / (double) allCandidatesMentionsCount)
                            .boxed()
                            .collect(Collectors.toList());

                    List<Double> candidatesRatings = IntStream.range(0, candidatesMatches.size())
                            .mapToDouble(i -> candidatesMatches.get(i) + config.getPopularityRate() * candidatesProbabilities.get(i))
//                            .mapToDouble(i -> config.getPopularityRate() * getScalledProbability(candidatesProbabilities.get(i)) + getScalledProbability(candidatesMatches.get(i)))
                            .boxed()
                            .collect(Collectors.toList());
                    idxOfBestCandidate = getMaxIdxFromList(candidatesRatings);
                } else {
                    idxOfBestCandidate = getMaxIdxFromList(candidatesMatches);
                }
                resultList.add(idxOfBestCandidate == -1 ? null : candidates.get(idxOfBestCandidate));
            });
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return resultList;
    }

    private Path prepareFileForBert(List<Pair<NamedEntity, List<WikiItemEntity>>> candidatesForMentions) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        int candidatesForMentionsSize = candidatesForMentions.size();
        AtomicInteger processedSize = new AtomicInteger(0);
        candidatesForMentions.forEach(pair -> {
            NamedEntity namedEntity = pair.getFirst();
            List<WikiItemEntity> candidates = pair.getSecond();
            candidates.forEach(candidate ->
                    stringBuffer.append(BertIntegrationUtils.prepareExampleForClassifier(namedEntity.getPageId(),
                            namedEntity, candidate, config.getArticlePartSize(), config.getArticlesDirectory()))
            );
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
                log.info("Waiting for results ...");
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

    private Double getFirstNumberInTsvLine(String line) {
        return Double.valueOf(line.split("\\t")[0]);
    }

    private int getMaxIdxFromList(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }
        Double maxVal = values.stream().reduce(Double::max).get();
        return values.indexOf(maxVal);
    }

    private Double getScalledProbability(Double probabilityValue) {
        if (probabilityValue == 1D) {
            return Double.MAX_VALUE;
        }
        return 1 / (1 - probabilityValue);
    }


    private Integer getMentionsCount(WikiItemEntity wikiItem) {
        return Optional.ofNullable(wikiItem.getMentionsCount()).orElse(0);
    }
}
