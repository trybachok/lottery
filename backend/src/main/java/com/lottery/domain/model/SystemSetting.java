package com.lottery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SystemSetting(String key, String valueJson, UUID updatedBy, Instant updatedAt) {
    public SystemSetting {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueJson, "valueJson");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (key.isBlank()) {
            throw new IllegalArgumentException("System setting key must not be blank");
        }
    }

    public Optional<UUID> maybeUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }
}
