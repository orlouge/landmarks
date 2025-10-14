package io.github.orlouge.landmarks.utils;

import java.util.*;
import java.util.regex.*;

public class StringUtils {
    public static List<String> splitWithDelimiters(String input, String regex) {
        if (input == null || input.isEmpty()) return List.of();
        if (regex == null || regex.isEmpty()) return List.of(input);

        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        int lastEnd = 0;
        while (matcher.find()) {
            result.add(input.substring(lastEnd, matcher.start()));
            result.add(matcher.group());
            lastEnd = matcher.end();
        }

        result.add(input.substring(lastEnd));

        return result;
    }
}