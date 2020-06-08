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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Primary
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
            Path uploadPath = prepareFileForBert(candidatesForMentions);
            String fileName = uploadPath.getFileName().toString();

            BertIntegrationUtils.uploadObject(config.getGcpProjectId(), config.getGsBucketName(),
                    getGsPath(config.getGsInputDir(), fileName), uploadPath);
            Path downloadPath = waitForResultsAndDownloadWhereReady(fileName);
            List<Float> contextMatches = Files.lines(downloadPath)
                    .map(this::getFirstNumberInTsvLine)
                    .collect(Collectors.toList());
            AtomicInteger currentIdx = new AtomicInteger(0);
            candidatesForMentions.forEach(pair -> {
                List<WikiItemEntity> candidates = pair.getSecond();
                List<Float> candidatesMatches =
                        contextMatches.subList(currentIdx.get(), currentIdx.addAndGet(candidates.size()));
                int idxOfBestCandidate = getMaxIdxFromList(candidatesMatches);
                resultList.add(candidates.get(idxOfBestCandidate));
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

    private Float getFirstNumberInTsvLine(String line) {
        return Float.valueOf(line.split("\\t")[0]);
    }

    private int getMaxIdxFromList(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }
        Float maxVal = values.stream().reduce(Float::max).get();
        return values.indexOf(maxVal);
    }
}
