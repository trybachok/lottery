package com.lottery.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Permission(UUID id, String code, String description) {
    public Permission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Permission code must not be blank");
        }
    }
}
