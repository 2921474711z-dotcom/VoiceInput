package com.voiceinput.pro.model.dto;

import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskSummaryResponse(
    String id,
    String title,
    SceneType sceneType,
    TaskStatus status,
    String fileName,
    String templateId,
    String templateName,
    Integer optimizedWordCount,
    Integer rawWordCount,
    Integer hotwordHitCount,
    BigDecimal estimatedCost,
    Long totalDurationMs,
    Boolean savedToHistory,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
}
