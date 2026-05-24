package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateExportRequest(
    @NotBlank String taskId,
    @NotBlank String exportType,
    @NotBlank String contentSource
) {
}
