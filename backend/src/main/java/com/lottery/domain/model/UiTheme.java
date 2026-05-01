package com.lottery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UiTheme(UUID id, String name, String tokensJson, boolean defaultTheme, Instant createdAt) {
    public UiTheme {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tokensJson, "tokensJson");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
