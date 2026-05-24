package com.voiceinput.pro.model.dto;

public record HotwordCategoryResponse(
    Long id,
    String code,
    String name,
    String icon,
    Integer sortOrder,
    Boolean enabled,
    Long count
) {
}

