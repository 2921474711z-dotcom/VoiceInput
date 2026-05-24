package com.voiceinput.pro.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UploadAssetResponse(
    String id,
    String fileName,
    long sizeBytes,
    BigDecimal durationSeconds,
    LocalDateTime uploadedAt
) {
}

