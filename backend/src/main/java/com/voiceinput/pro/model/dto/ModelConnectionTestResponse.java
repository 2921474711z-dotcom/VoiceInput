package com.voiceinput.pro.model.dto;

import java.time.LocalDateTime;

public record ModelConnectionTestResponse(
    String id,
    String target,
    String provider,
    String baseUrl,
    String modelName,
    String status,
    String message,
    Long durationMs,
    String uploadId,
    LocalDateTime createdAt
) {
}
