package pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bert")
public class BertDisambiguatorConfig {

    private String gcpProjectId;
    private String gsBucketName;
    private String localUploadDir;
    private String localDownloadDir;
    private String gsInputDir;
    private String gsResultDir;
    private String gsSuccessDir;
    private String gsErrorDir;
    private String articlesDirectory;
    private int articlePartSize;
    private boolean useReadyPredictions;
    private String predictionsPath;
    private boolean usePopularity;
    private float popularityRate;
}
