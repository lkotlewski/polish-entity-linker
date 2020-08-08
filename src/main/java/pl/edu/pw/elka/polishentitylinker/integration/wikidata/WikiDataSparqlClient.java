package pl.edu.pw.elka.polishentitylinker.integration.wikidata;

import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.integration.wikidata.ex.SparqlQueryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiDataSparqlClient {

    private static final String INSTANCES_OF_SUBCLASSES_QUERY_PATTERN =
            "SELECT ?item WHERE { " +
                    "?item wdt:P31/wdt:P279+ wd:%s." +
                    "?sitelink schema:isPartOf <https://pl.wikipedia.org/>;" +
                    "schema:about ?item.}";
    private static final String ALL_LEVEL_SUBCLASSES_QUERY_PATTERN =
            "SELECT ?item WHERE { " +
                    "?item wdt:P279+ wd:%s." +
                    "?sitelink schema:isPartOf <https://pl.wikipedia.org/>;" +
                    "schema:about ?item.}";
    private static final String SUBCLASSES_QUERY_PATTERN = "SELECT ?item WHERE { ?item wdt:P279 wd:%s.}";

    private static final String FIRST_LINE_REGEX = "item\r\n";
    private static final String LINE_SUFFIX = "\r\n";
    private static final String ENTITY_BASE_URL = "http://www\\.wikidata\\.org/entity/";

    private static final String STACK_TRACE_SUFFIX = ")\n";

    public static final String INSTANCES_FILENAME_PATTERN = "%s_instances.tsv";
    public static final String SUBCLASSES_FILENAME_PATTERN = "%s_subclasses.tsv";


    @Value("${categories.files.directory}")
    private String directoryOut;

    @Value("${categories.download.limit}")
    private int downloadLimit;

    @Value("${categories.download.in-parts}")
    private boolean downloadInParts;

    @Value("${categories.download.max-attempts}")
    private int maxAttempts;

    @Value("#{'${categories.download.ignore-list}'.split(',')}")
    List<String> ignoreList;

    private Set<String> processedCategories;


    private final WikiDataSparqlRestClient wikiDataSparqlRestClient;


    public void downloadInstancesOf(String wikiClassId) {
        log.info("Downloading instances of {}", wikiClassId);
        processedCategories = new HashSet<>();
        downloadResults(wikiClassId, text -> String.format(INSTANCES_OF_SUBCLASSES_QUERY_PATTERN, text),
                INSTANCES_FILENAME_PATTERN);

    }

    public void downloadSubclassesOf(String wikiClassId) {
        log.info("Downloading subclasses of {}", wikiClassId);
        processedCategories = new HashSet<>();
        downloadResults(wikiClassId, text -> String.format(ALL_LEVEL_SUBCLASSES_QUERY_PATTERN, text),
                SUBCLASSES_FILENAME_PATTERN);
    }

    private void downloadResults(String wikiClassId, Function<String, String> queryPreparator, String filenamePattern) {
        String result = removeAllBaseUrls(getResult(wikiClassId, queryPreparator));
        if (isResultValid(result)) {
            result = removeDuplicates(result);
            writeResultToFile(wikiClassId, filenamePattern, result);
        }
    }

    private String getResult(String wikiClassId, Function<String, String> queryPreparator) {
        processedCategories.add(wikiClassId);
        log.info("Executing query for {}", wikiClassId);
        String query = queryPreparator.apply(wikiClassId);
        String result = retryableRequestForResults(maxAttempts, query);
        if (isResultValid(result)) {
            return result;
        } else {
            log.warn("Fail during download {}, trying with decomposition", wikiClassId);
            StringBuilder stringBuilder = new StringBuilder();
            List<String> subclasses = getSubclasses(wikiClassId);
            subclasses.forEach(classId -> {
                        if (!processedCategories.contains(classId)) {
                            stringBuilder.append(getResult(classId, queryPreparator));
                        }
                    }
            );
            return stringBuilder.toString();
        }
    }

    private List<String> getSubclasses(String wikiClassId) {
        String query = String.format(SUBCLASSES_QUERY_PATTERN, wikiClassId);
        String result = retryableRequestForResults(maxAttempts, query);
        if (!isResultValid(result)) {
            throw new SparqlQueryException(String.format("Error while subclasses query for %s", wikiClassId));
        }
        List<String> subclasses = Arrays.asList(result.split(LINE_SUFFIX));
        return subclasses.stream()
                .map(subclass -> subclass.replaceFirst(ENTITY_BASE_URL, ""))
                .filter(subclass -> !ignoreList.contains(subclass))
                .collect(Collectors.toList());
    }

    private void writeResultToFile(String wikiClassId, String filenamePattern, String result) {
        try {
            Files.write(Paths.get(directoryOut, String.format(filenamePattern, wikiClassId)),
                    result.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String retryableRequestForResults(int maxAttempts, String query) {
        boolean queryExecuted = false;
        String response = null;
        int attempts = 0;
        while (!queryExecuted && attempts < maxAttempts) {
            try {
                attempts += 1;
                response = executeQuery(query);
                queryExecuted = true;
            } catch (RetryableException e) {
                log.error(e.getMessage());
                queryExecuted = false;
            }
        }
        return response;
    }

    private String executeQuery(String query) {
        return removeFirstLine(wikiDataSparqlRestClient.executeQuery(query));
    }

    private String removeFirstLine(String s) {
        return s != null ? s.replaceFirst(FIRST_LINE_REGEX, "") : null;
    }

    private String removeAllBaseUrls(String s) {
        return s.replaceAll(ENTITY_BASE_URL, "");
    }

    private String removeDuplicates(String result) {
        return String.join(LINE_SUFFIX, new HashSet<>(Arrays.asList(result.split(LINE_SUFFIX))));
    }

    private boolean isResultValid(String result) {
        return result != null && !result.endsWith(STACK_TRACE_SUFFIX);
    }
}
