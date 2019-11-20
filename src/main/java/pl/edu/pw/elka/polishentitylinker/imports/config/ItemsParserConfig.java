package pl.edu.pw.elka.polishentitylinker.imports.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "import")
public class ItemsParserConfig {

    private String wikiItemsFilepath;
    private String pagesFilepath;
    private String redirectFilepath;
    private String tokensWithEntitiesFilepath;
    private int saveBatchSize;
}
