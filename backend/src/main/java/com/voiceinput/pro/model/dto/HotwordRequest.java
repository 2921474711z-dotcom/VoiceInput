package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HotwordRequest(
    @NotBlank String recognizedTerm,
    @NotBlank String standardTerm,
    @NotNull Long categoryId,
    @NotEmpty List<String> scenes,
    @NotNull Boolean enabled,
    List<HotwordSampleRequest> samples
) {
}

