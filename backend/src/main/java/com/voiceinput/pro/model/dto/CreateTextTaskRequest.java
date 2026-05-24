package com.voiceinput.pro.model.dto;

import com.voiceinput.pro.model.enums.SceneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTextTaskRequest(
    String uploadId,
    @NotNull SceneType sceneType,
    @NotBlank String templateId,
    @NotBlank String rawText
) {
}
