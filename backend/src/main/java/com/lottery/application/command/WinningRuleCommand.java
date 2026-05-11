package com.lottery.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record WinningRuleCommand(
        BigDecimal matchPercentFrom,
        BigDecimal matchPercentTo,
        UUID prizeId,
        int priority) {
}
