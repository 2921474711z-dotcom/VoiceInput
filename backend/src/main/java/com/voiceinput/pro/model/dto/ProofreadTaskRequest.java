package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ProofreadTaskRequest(
    @NotBlank String rawText,
    @NotBlank String optimizedText,
    String markdownContent
) {
}
