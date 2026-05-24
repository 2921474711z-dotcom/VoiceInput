package com.voiceinput.pro.model.dto;

import jakarta.validation.constraints.NotBlank;

public record HotwordSampleRequest(
    @NotBlank String sampleBefore
) {
}

