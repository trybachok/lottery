package com.lottery.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DrawResultDto(
        UUID id,
        UUID drawId,
        List<String> winningCombinationValues,
        String algorithmVersion,
        String randomProvider,
        String proofHash,
        UUID generatedBy,
        Instant generatedAt,
        String requestId,
        String correlationId) {
    public DrawResultDto {
        winningCombinationValues = List.copyOf(winningCombinationValues);
    }
}
