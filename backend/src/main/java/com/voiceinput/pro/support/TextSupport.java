package com.voiceinput.pro.support;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class TextSupport {

    private TextSupport() {
    }

    public static int countMeaningfulCharacters(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
    }

    public static String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return Arrays.stream(text.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));
    }

    public static String buildTitle(String text) {
        String normalized = normalizeWhitespace(text);
        if (normalized.isBlank()) {
            return "未命名记录";
        }
        return normalized.length() > 24 ? normalized.substring(0, 24) + "..." : normalized;
    }
}

