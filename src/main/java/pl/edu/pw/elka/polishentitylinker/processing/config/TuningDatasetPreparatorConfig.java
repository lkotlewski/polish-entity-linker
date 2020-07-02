package pl.edu.pw.elka.polishentitylinker.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tuning-dataset")
public class TuningDatasetPreparatorConfig {

    private String trainFilepath;
    private String devFilepath;
    private String testFilepath;
    private String articlesDirectory;
    private int articlePartSize;
    private int trainArticleMinLength;
    private int trainExamplesCount;
    private int devExamplesCount;
    private int testExamplesCount;
}
