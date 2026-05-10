package com.lottery.application.command;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreateDrawCommand(
        String title,
        String description,
        UUID managerId,
        UUID combinationSchemaId,
        CreateCombinationSchemaCommand combinationSchema,
        Instant salesStartAt,
        Instant salesEndAt,
        Instant drawAt,
        Integer maxTickets,
        boolean test) {
    public CreateDrawCommand {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(salesStartAt, "salesStartAt");
        Objects.requireNonNull(salesEndAt, "salesEndAt");
        Objects.requireNonNull(drawAt, "drawAt");
        if (combinationSchemaId == null && combinationSchema == null) {
            throw new IllegalArgumentException("Either combinationSchemaId or combinationSchema is required");
        }
        if (combinationSchemaId != null && combinationSchema != null) {
            throw new IllegalArgumentException("Use either combinationSchemaId or combinationSchema, not both");
        }
    }
}
