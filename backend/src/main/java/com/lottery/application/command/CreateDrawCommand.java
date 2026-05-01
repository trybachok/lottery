package com.lottery.application.command;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreateDrawCommand(
        String title,
        String description,
        UUID managerId,
        UUID combinationSchemaId,
        Instant salesStartAt,
        Instant salesEndAt,
        Instant drawAt,
        Integer maxTickets,
        boolean test) {
    public CreateDrawCommand {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(combinationSchemaId, "combinationSchemaId");
        Objects.requireNonNull(salesStartAt, "salesStartAt");
        Objects.requireNonNull(salesEndAt, "salesEndAt");
        Objects.requireNonNull(drawAt, "drawAt");
    }
}
