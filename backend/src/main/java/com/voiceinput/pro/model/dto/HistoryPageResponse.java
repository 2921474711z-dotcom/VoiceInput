package com.voiceinput.pro.model.dto;

import java.util.List;

public record HistoryPageResponse(
    List<TaskSummaryResponse> items,
    long total,
    int page,
    int size
) {
}

