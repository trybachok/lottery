package com.lottery.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DrawDto(
        UUID id,
        String title,
        String description,
        String status,
        UUID managerId,
        UUID combinationSchemaId,
        Instant salesStartAt,
        Instant salesEndAt,
        Instant drawAt,
        Integer maxTickets,
        boolean test,
        Instant createdAt,
        long version) {
}
