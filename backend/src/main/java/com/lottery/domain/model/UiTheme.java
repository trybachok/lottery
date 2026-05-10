package com.lottery.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record UiTheme(
        UUID id,
        String name,
        Map<String, Object> tokens,
        boolean defaultTheme,
        Instant createdAt) {
    public UiTheme {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Theme name must not be blank");
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Theme tokens must not be empty");
        }
        tokens = Map.copyOf(tokens);
    }
}
