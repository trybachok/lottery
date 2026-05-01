package com.lottery.domain.valueobject;

import java.util.Objects;

public record CombinationSchemaDefinition(String document) {
    public CombinationSchemaDefinition {
        Objects.requireNonNull(document, "document");
        if (document.isBlank()) {
            throw new IllegalArgumentException("Combination schema document must not be blank");
        }
    }
}
