package pl.edu.pw.elka.polishentitylinker.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "article-extract")
public class ArticleToFileExtractorConfig {

    private String corpusFilepath;
    private String outDirectoryFilepath;
}
