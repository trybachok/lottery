package com.lottery.domain.model;

import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CombinationSchema(UUID id, String name, CombinationSchemaDefinition definition, Instant createdAt) {
    public CombinationSchema {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Combination schema name must not be blank");
        }
    }
}
