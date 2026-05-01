package com.lottery.application.query;

public record ListDrawsQuery(int limit, int offset) {
    public ListDrawsQuery {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
