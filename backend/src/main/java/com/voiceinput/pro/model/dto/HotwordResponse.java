package com.voiceinput.pro.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HotwordResponse(
    String id,
    String recognizedTerm,
    String standardTerm,
    Long categoryId,
    String categoryName,
    List<String> scenes,
    Boolean enabled,
    List<HotwordSampleResponse> samples,
    LocalDateTime createdAt
) {
}

