package com.voiceinput.pro.model.dto;

import java.time.LocalDateTime;

public record ProofreadTaskResponse(
    String taskId,
    String proofreadRevisionId,
    String rawText,
    String optimizedText,
    String markdownContent,
    LocalDateTime proofreadAt
) {
}
