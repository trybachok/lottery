package com.lottery.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record WinningRule(
        UUID id,
        UUID drawId,
        BigDecimal matchPercentFrom,
        BigDecimal matchPercentTo,
        UUID prizeId,
        int priority) {
    public WinningRule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(drawId, "drawId");
        Objects.requireNonNull(matchPercentFrom, "matchPercentFrom");
        Objects.requireNonNull(matchPercentTo, "matchPercentTo");
        Objects.requireNonNull(prizeId, "prizeId");
        if (matchPercentFrom.compareTo(BigDecimal.ZERO) < 0 || matchPercentTo.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Winning rule percent range must be between 0 and 100");
        }
        if (matchPercentFrom.compareTo(matchPercentTo) > 0) {
            throw new IllegalArgumentException("matchPercentFrom must not be greater than matchPercentTo");
        }
    }
}
