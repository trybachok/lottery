package com.lottery.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Prize(
        UUID id,
        String type,
        String name,
        BigDecimal amount,
        Currency currency,
        UUID productId,
        BigDecimal quantity,
        String unit) {
    public Prize {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        if (type.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Prize type and name must not be blank");
        }
    }

    public Optional<BigDecimal> maybeAmount() {
        return Optional.ofNullable(amount);
    }
}
