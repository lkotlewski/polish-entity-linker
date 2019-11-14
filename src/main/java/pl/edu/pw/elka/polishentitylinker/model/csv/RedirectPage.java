package pl.edu.pw.elka.polishentitylinker.model.csv;

import lombok.Data;
import pl.edu.pw.elka.polishentitylinker.utils.CsvLineParser;

import java.util.List;

@Data
public class RedirectPage {

    private int pageId;
    private String targetTitle;

    public RedirectPage(String line) {
        List<String> parts = CsvLineParser.parse(line);
        pageId = Integer.parseInt(parts.get(0));
        targetTitle = parts.get(2).replace("\"", "");
    }
}
