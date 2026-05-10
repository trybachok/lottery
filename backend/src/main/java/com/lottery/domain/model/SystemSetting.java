package com.lottery.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SystemSetting(
        String key,
        Map<String, Object> value,
        UUID updatedBy,
        Instant updatedAt) {
    public SystemSetting {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Setting key must not be blank");
        }
        value = Map.copyOf(value);
    }
}
