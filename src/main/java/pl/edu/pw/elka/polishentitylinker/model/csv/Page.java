package pl.edu.pw.elka.polishentitylinker.model.csv;


import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.utils.CsvLineParser;

import java.util.List;

@Data
public class Page {

    private int pageId;
    private String title;
    private PageType type;

    public Page(String line) {
        List<String> parts = CsvLineParser.parse(line);
        pageId = Integer.parseInt(parts.get(0));
        title = parts.get(1).replace("\"", "");
        type = PageType.valueOf((Integer.parseInt(parts.get(2))));
    }
}
