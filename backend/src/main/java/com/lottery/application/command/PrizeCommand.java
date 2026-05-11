package com.lottery.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record PrizeCommand(
        String type,
        String name,
        BigDecimal amount,
        String currency,
        UUID productId,
        BigDecimal quantity,
        String unit) {
}
