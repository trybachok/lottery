package com.lottery.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WinningRuleDto(
        UUID id,
        UUID drawId,
        BigDecimal matchPercentFrom,
        BigDecimal matchPercentTo,
        UUID prizeId,
        int priority) {
}
