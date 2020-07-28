package pl.edu.pw.elka.polishentitylinker.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "entity-linker")
public class EntityLinkerConfig {

    private String testFilepath;
    private String outFilepath;
    private String candidatesFilepath;
    private boolean doSearch;
    private boolean doDisambiguate;
    private boolean limitSearchResults;
    private int searchResultsLimit;
}
