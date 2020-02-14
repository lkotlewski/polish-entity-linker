package pl.edu.pw.elka.polishentitylinker.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "train-test")
public class TestTrainPreparatorConfig {

    private String testFilepath;
    private String trainDirectory;
    private String backupDirectory;
    private float trainLeftPart;
    private int numberToDivide;
}
