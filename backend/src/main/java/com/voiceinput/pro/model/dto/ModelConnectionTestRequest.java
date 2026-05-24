package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ModelConnectionTestRequest(
    @NotBlank String target,
    String uploadId
) {
}
