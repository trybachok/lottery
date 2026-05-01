package com.lottery.application.query;

import com.lottery.domain.valueobject.DrawStatus;
import java.time.Instant;
import java.util.UUID;

public record DrawReportQuery(UUID drawId, UUID userId, DrawStatus status, Instant dateFrom, Instant dateTo, int limit, int offset) {
    public DrawReportQuery {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }
    }
}
