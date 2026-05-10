package com.lottery.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record UiTemplate(
        UUID id,
        String name,
        Map<String, Object> layout,
        Instant createdAt) {
    public UiTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be blank");
        }
        if (layout.isEmpty()) {
            throw new IllegalArgumentException("Template layout must not be empty");
        }
        layout = Map.copyOf(layout);
    }
}
