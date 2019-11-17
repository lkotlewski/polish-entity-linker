package pl.edu.pw.elka.polishentitylinker.model.csv;

import java.util.Optional;
import java.util.stream.Stream;

public enum PageType {
    REGULAR_ARTICLE(0),
    CATEGORY(1),
    REDIRECT(2),
    DISAMBIGUATION(3),
    TEMPLATE(4),
    OTHER(5);

    private final int value;

    PageType(int value) {
        this.value = value;
    }

    public static PageType valueOf(int intValue) {
        Optional<PageType> result = Stream.of(PageType.values())
                .filter(pageType -> intValue == pageType.value)
                .findAny();
        return result.orElseGet(null);
    }

    public int getValue() { return value; }
}
