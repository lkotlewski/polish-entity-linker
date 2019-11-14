package pl.edu.pw.elka.polishentitylinker.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.edu.pw.elka.polishentitylinker.entities.RedirectPageEntity;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.imports.config.ItemsParserConfig;
import pl.edu.pw.elka.polishentitylinker.model.csv.RedirectPage;
import pl.edu.pw.elka.polishentitylinker.repository.RedirectPageRepository;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;
import pl.edu.pw.elka.polishentitylinker.utils.BufferedBatchProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedirectPageProcessor extends LineFileProcessor {

    private final ItemsParserConfig itemsParserConfig;
    private final WikiItemRepository wikiItemRepository;
    private final RedirectPageRepository redirectPageRepository;

    private BufferedBatchProcessor<RedirectPageEntity> redirectPageUpdater;

    @Override
    public void parseFile() {
        redirectPageUpdater = new BufferedBatchProcessor<>(redirectPageRepository::saveAll, itemsParserConfig.getSaveBatchSize());
        parseFile(itemsParserConfig.getRedirectFilepath(), this::readRedirectPageLine);
        redirectPageUpdater.processRest();
    }

    private void readRedirectPageLine(String line) {
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
