package pl.edu.pw.elka.polishentitylinker.service.disambiguator.bert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.utils.BertIntegrationUtils;

import java.io.IOException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class BertService {

    private final BertDisambiguatorConfig config;

    public boolean exists(String gsFilepath) {
        return BertIntegrationUtils.exists(config.getGcpProjectId(), config.getGsBucketName(), gsFilepath);
    }

    public void upload(String gsFilepath, Path localFilePath) throws IOException {
        BertIntegrationUtils.uploadObject(config.getGcpProjectId(), config.getGsBucketName(), gsFilepath, localFilePath);
    }

    public void download(String gsFilepath, Path localFilePath) {
        BertIntegrationUtils.downloadObject(config.getGcpProjectId(), config.getGsBucketName(), gsFilepath, localFilePath);
    }
}
