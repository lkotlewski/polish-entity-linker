package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CsvLineParser {

    private static final String SPLITTING_COMMA_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    public static List<String> parse(String line) {
        return Arrays.asList(line.split(SPLITTING_COMMA_REGEX));
    }
}
