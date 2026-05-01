package com.lottery.application.query;

import java.time.Instant;
import java.util.UUID;

public record ListAuditLogsQuery(
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Instant dateFrom,
        Instant dateTo,
        int limit,
        int offset) {
    public ListAuditLogsQuery {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }
        action = blankToNull(action);
        entityType = blankToNull(entityType);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
