package com.voiceinput.pro.model.dto;

import java.time.LocalDateTime;

public record ExportRecordResponse(
    String id,
    String taskId,
    String taskTitle,
    String fileName,
    String exportType,
    String contentType,
    String contentSource,
    Long sizeBytes,
    String status,
    String errorMessage,
    LocalDateTime createdAt
) {
}
