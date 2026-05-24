package com.voiceinput.pro.model.dto;

public record TrendPointResponse(
    String date,
    long taskCount,
    double averageCost
) {
}

