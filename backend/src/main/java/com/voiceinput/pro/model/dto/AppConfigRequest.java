package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppConfigRequest(
    @NotBlank String recognitionModel,
    @NotBlank String languageType,
    @NotBlank String domainModel,
    @NotBlank String outputFormat,
    @NotBlank String stabilityMode,
    @NotBlank String optimizationModel,
    @NotBlank String optimizationGoal,
    @NotBlank String toneStyle,
    @NotBlank String lengthPreference,
    @NotNull Boolean hotwordEnabled,
    @NotBlank String costMode,
    @NotBlank String asrModelRoute,
    @NotBlank String llmModelRoute
) {
}
