package com.lottery.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RunDrawResultDto(
        UUID drawId,
        UUID drawResultId,
        List<String> winningCombinationValues,
        int processedTickets,
        int winningTickets,
        int losingTickets,
        Instant completedAt) {
    public RunDrawResultDto {
        winningCombinationValues = List.copyOf(winningCombinationValues);
    }
}
