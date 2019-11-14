package pl.edu.pw.elka.polishentitylinker.utils;

import java.util.Arrays;
import java.util.List;

public class CsvLineParser {

    private static final String SPLITTING_COMMA_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    public static List<String> parse(String line) {
        return Arrays.asList(line.split(SPLITTING_COMMA_REGEX));
    }
}
