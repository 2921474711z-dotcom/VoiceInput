package com.voiceinput.pro.model.dto;

public record HotwordMatchResponse(
    String recognizedTerm,
    String standardTerm,
    int hitCount
) {
}

