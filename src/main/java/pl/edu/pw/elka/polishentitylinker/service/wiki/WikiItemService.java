package pl.edu.pw.elka.polishentitylinker.service.wiki;

import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;

public interface WikiItemService {

    WikiItem add(WikiItem wikiItem);

    WikiItem findByTitle(String title);
}
