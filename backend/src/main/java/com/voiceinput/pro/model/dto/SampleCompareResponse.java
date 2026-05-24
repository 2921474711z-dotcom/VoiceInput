package com.voiceinput.pro.model.dto;

public record SampleCompareResponse(
    String id,
    String title,
    String rawText,
    String optimizedText,
    String improvement
) {
}
