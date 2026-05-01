package com.lottery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UiTemplate(UUID id, String name, String layoutJson, Instant createdAt) {
    public UiTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(layoutJson, "layoutJson");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
