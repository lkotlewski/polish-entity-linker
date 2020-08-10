package pl.edu.pw.elka.polishentitylinker.processing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entity.RedirectPageEntity;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.csv.RedirectPage;
import pl.edu.pw.elka.polishentitylinker.processing.LineFileProcessor;
import pl.edu.pw.elka.polishentitylinker.processing.config.BatchProcessingConfig;
import pl.edu.pw.elka.polishentitylinker.processing.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.repository.RedirectPageRepository;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.processing.BufferedBatchProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedirectPageProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final BatchProcessingConfig batchProcessingConfig;
    private final WikiItemRepository wikiItemRepository;
    private final RedirectPageRepository redirectPageRepository;

    private BufferedBatchProcessor<RedirectPageEntity> redirectPageUpdater;

    @Override
    public void processFile() {
        redirectPageUpdater = new BufferedBatchProcessor<>(redirectPageRepository::saveAll, batchProcessingConfig.getSize());
        processLineByLine(itemsParserConfig.getRedirectFilepath());
        redirectPageUpdater.processRest();
    }

    @Override
    protected void processLine(String line) {
        RedirectPage redirectPage = new RedirectPage(line);
        log.info(String.valueOf(redirectPage.getPageId()));
        WikiItemEntity wikiItemEntity = wikiItemRepository.findByTitlePl(redirectPage.getTargetTitle());

        RedirectPageEntity redirectPageEntity = new RedirectPageEntity();
        redirectPageEntity.setId(redirectPage.getPageId());
        redirectPageEntity.setLabel(redirectPage.getTargetTitle());
        redirectPageEntity.setTarget(wikiItemEntity);

        redirectPageUpdater.process(redirectPageEntity);
    }
}
