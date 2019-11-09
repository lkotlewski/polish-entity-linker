package pl.edu.pw.elka.polishentitylinker.model.csv;


import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Page {

    private static final String QUOTED_TITLE_REGEX = "\".*\"";
    private static final Pattern pattern = Pattern.compile(QUOTED_TITLE_REGEX);

    private int pageId;
    private String title;
    private PageType type;

    public Page(String line) {
        if (line != null) {
            if (line.contains("\"")) {
                parseLineWithQouts(line);
            } else {
                parseStandardLine(line);
            }
        }
    }

    private void parseStandardLine(String line) {
        List<String> parts = Arrays.asList(line.split(","));
        pageId = Integer.parseInt(parts.get(0));
        title = parts.get(1);
        type = PageType.valueOf((Integer.parseInt(parts.get(2))));
    }

    private void parseLineWithQouts(String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            title = matcher.group(0).replace("\"", "");
        }

        String withEmptyTitle = line.replaceFirst(QUOTED_TITLE_REGEX, "");
        List<String> parts = Arrays.asList(withEmptyTitle.split(","));
        pageId =  Integer.parseInt(parts.get(0));
        type = PageType.valueOf((Integer.parseInt(parts.get(2))));
    }
}
