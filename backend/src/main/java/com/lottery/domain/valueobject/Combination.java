package com.lottery.domain.valueobject;

import java.util.List;
import java.util.Objects;

public record Combination(List<String> values) {
    public Combination {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Combination must contain at least one value");
        }
        values = List.copyOf(values);
    }
}
