package com.voiceinput.pro.model.dto;

import java.time.LocalDateTime;

public record ConfigTemplateResponse(
    String id,
    String name,
    String description,
    Boolean defaultTemplate,
    AppConfigRequest config,
    LocalDateTime createdAt
) {
}

