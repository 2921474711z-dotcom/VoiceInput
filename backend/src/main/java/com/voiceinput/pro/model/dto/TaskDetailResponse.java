package com.voiceinput.pro.model.dto;

import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
    String id,
    String sourceTaskId,
    Integer versionIndex,
    String title,
    String summary,
    SceneType sceneType,
    TaskStatus status,
    String fileName,
    String templateId,
    String templateName,
    String rawText,
    String optimizedText,
    String markdownContent,
    Integer rawWordCount,
    Integer optimizedWordCount,
    Integer hotwordHitCount,
    BigDecimal estimatedCost,
    Long recognitionDurationMs,
    Long optimizationDurationMs,
    Long totalDurationMs,
    Boolean savedToHistory,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    List<HotwordMatchResponse> hotwordMatches
) {
}
