package com.lottery.application.command;

import java.time.Instant;
import java.util.UUID;

public record UpdateDrawCommand(
        UUID drawId,
        String title,
        String description,
        Instant salesStartAt,
        Instant salesEndAt,
        Instant drawAt,
        Integer maxTickets) {
}
