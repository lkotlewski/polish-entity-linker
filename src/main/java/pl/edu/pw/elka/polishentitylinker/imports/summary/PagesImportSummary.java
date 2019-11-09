package pl.edu.pw.elka.polishentitylinker.imports.summary;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pl.edu.pw.elka.polishentitylinker.model.csv.PageType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@Setter
@Slf4j
public class PagesImportSummary {

    Map<PageType, Integer> countsByType = new HashMap<>();

    public PagesImportSummary() {
        Stream.of(PageType.values())
                .forEach(value -> countsByType.put(value, 0));
    }

    public void incrementCounts(PageType pageType) {
        if (pageType != null) {
            countsByType.put(pageType, countsByType.get(pageType) + 1);
        }
    }

    public void printSummary() {
        countsByType.forEach((k, v) -> log.info(String.format("%s : %d", k.toString(), v)));
    }
}
