package pl.edu.pw.elka.polishentitylinker.integration.gcp;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.model.tsv.TokenizedWord;
import pl.edu.pw.elka.polishentitylinker.utils.TokenizedTextUtils;
import pl.edu.pw.elka.polishentitylinker.utils.TsvLineParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BertIntegrationUtils {

    private static final String LINE_PATTERN = "%s\t%d\t%s\t%s\t%s\t%s\t%b\n";

    public static boolean exists(String projectId, String bucketName, String gsFilepath) {
        return !listObjectsWithPrefix(projectId, bucketName, gsFilepath).isEmpty();
    }

    public static List<Blob> listObjectsWithPrefix(
            String projectId, String bucketName, String directoryPrefix) {

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Iterable<Blob> values = bucket.list(
                Storage.BlobListOption.prefix(directoryPrefix),
                Storage.BlobListOption.currentDirectory()).getValues();
        return StreamSupport.stream(values.spliterator(), false)
                .collect(Collectors.toList());
    }

    public static void downloadObject(
            String projectId, String bucketName, String objectName, Path destFilePath) {
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        blob.downloadTo(destFilePath);
        log.info("Downloaded object " + objectName + " from bucket name " + bucketName + " to " + destFilePath);
    }

    public static void uploadObject(
            String projectId, String bucketName, String objectName, Path filePath) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(filePath));
        log.info("File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }

    public static String prepareExampleForClassifier(Integer contextPageId, NamedEntity targetNamedEntity,
            WikiItemEntity candidateWikiItem, int articlePartSize, String articlesDirectory) {
        return prepareDatasetLine(
                targetNamedEntity.toOriginalForm(),
                contextPageId,
                targetNamedEntity.getEntityId(),
                candidateWikiItem.getId(),
                targetNamedEntity.getContextAsString(),
                getArticleBeginningByPageId(candidateWikiItem.getPageId(), articlePartSize, articlesDirectory),
                targetNamedEntity.getEntityId().equals(candidateWikiItem.getId())
        );
    }

    public static String prepeareEmptyExample() {
        return prepareDatasetLine(
                "0",
                0,
                "0",
                "0",
                "0",
                "0",
                true
        );
    }

    private static String prepareDatasetLine(String originalForm, Integer contextArticleId, String targetWikiItemId,
            String comparedWikiItemId, String context, String compared, boolean positiveExample) {
        return String.format(LINE_PATTERN, originalForm, contextArticleId, targetWikiItemId, comparedWikiItemId,
                context, compared, positiveExample);
    }

    private static String getArticleBeginningByPageId(Integer pageId, int articlePartSize, String articlesDirectory) {
        try {
            Path path = getArticlePath(pageId, articlesDirectory);
            if (path.toFile().exists()) {
                List<TokenizedWord> collect = Files.lines(path)
                        .map(TsvLineParser::parseTokenizedWord)
                        .filter(Objects::nonNull)
                        .limit(articlePartSize)
                        .collect(Collectors.toList());
                return TokenizedTextUtils.spanToOriginalForm(collect);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return "-";
    }

    private static Path getArticlePath(Integer pageId, String articlesDirectory) {
        return Paths.get(articlesDirectory, String.format("%d.tsv", pageId));
    }
}
