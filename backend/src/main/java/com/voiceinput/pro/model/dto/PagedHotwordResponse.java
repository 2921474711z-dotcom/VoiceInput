package com.voiceinput.pro.model.dto;

import java.util.List;

public record PagedHotwordResponse(
    List<HotwordResponse> items,
    long total,
    int page,
    int size
) {
}

