package com.lottery.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PrizeDto(
        UUID id,
        String type,
        String name,
        BigDecimal amount,
        String currency,
        UUID productId,
        BigDecimal quantity,
        String unit) {
}
