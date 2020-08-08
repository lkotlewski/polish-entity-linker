package pl.edu.pw.elka.polishentitylinker.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "results-evaluator")
public class FinalResultsEvaluatorConfig {

    private String candidatesFilepath;
    private String predictionsFilepath;
    private String evaluationsFilepath;
}
