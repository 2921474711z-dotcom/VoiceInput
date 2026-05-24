package com.voiceinput.pro.model.dto;

import java.math.BigDecimal;

public record AppConfigResponse(
    String recognitionModel,
    String languageType,
    String domainModel,
    String outputFormat,
    String stabilityMode,
    String optimizationModel,
    String optimizationGoal,
    String toneStyle,
    String lengthPreference,
    Boolean hotwordEnabled,
    String costMode,
    String asrModelRoute,
    String llmModelRoute,
    String asrProvider,
    String asrBaseUrl,
    String asrRuntimeModel,
    String llmProvider,
    String llmBaseUrl,
    String llmRuntimeModel,
    Integer modelTimeoutSeconds,
    Integer modelMaxRetries,
    BigDecimal estimatedSeconds,
    BigDecimal estimatedCostPerMinute,
    String defaultTemplateId,
    String defaultTemplateName
) {
}
