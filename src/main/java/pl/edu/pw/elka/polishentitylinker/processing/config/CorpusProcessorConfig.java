package pl.edu.pw.elka.polishentitylinker.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "corpus.processing")
public class CorpusProcessorConfig {

    @Value("${corpus.filepath}")
    String filepath;

    boolean countMentions;

    boolean evalArticlesLength;

    boolean extractAliases;

    boolean logProcessedDocsNumber;
}
