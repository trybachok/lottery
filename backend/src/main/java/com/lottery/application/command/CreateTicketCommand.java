package com.lottery.application.command;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateTicketCommand(
        UUID userId,
        UUID drawId,
        List<String> combinationValues,
        BigDecimal priceAmount,
        Currency priceCurrency,
        boolean test) {
    public CreateTicketCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(drawId, "drawId");
        Objects.requireNonNull(combinationValues, "combinationValues");
        Objects.requireNonNull(priceAmount, "priceAmount");
        Objects.requireNonNull(priceCurrency, "priceCurrency");
        combinationValues = List.copyOf(combinationValues);
    }
}
