package pl.edu.pw.elka.polishentitylinker.processing.config;

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
    private String trainDirectoryFilepath;
    private String testFilepath;
    private String backupFolderFilepath;
    private int saveBatchSize;
}
