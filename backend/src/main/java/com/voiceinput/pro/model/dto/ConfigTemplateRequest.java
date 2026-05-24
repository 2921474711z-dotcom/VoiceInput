package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigTemplateRequest(
    @NotBlank String name,
    String description,
    @NotNull AppConfigRequest config,
    @NotNull Boolean defaultTemplate
) {
}

