package com.lottery.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketDto(
        UUID id,
        UUID userId,
        UUID drawId,
        String status,
        List<String> combinationValues,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean test,
        Instant createdAt,
        long version) {
    public TicketDto {
        combinationValues = List.copyOf(combinationValues);
    }
}
