package com.lottery.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Role(UUID id, String code, String name, String description, boolean system) {
    public Role {
        Objects.requireNonNull(id, "id");
        requireNonBlank(code, "code");
        requireNonBlank(name, "name");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
