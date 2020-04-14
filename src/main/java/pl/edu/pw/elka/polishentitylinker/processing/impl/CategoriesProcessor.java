package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.client.WikiDataSparqlClient;
import pl.edu.pw.elka.polishentitylinker.processing.config.CategoriesProcessorConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoriesProcessor {

    private final CategoriesProcessorConfig config;
    private final WikiDataSparqlClient wikiDataSparqlClient;

    public void processCategoriesStructure() {
        config.getRootCategories().forEach(category -> {
            wikiDataSparqlClient.downloadInstancesOf(category);
            wikiDataSparqlClient.downloadSubclassesOf(category);

        });
        log.info("Process finished");
    }
}
