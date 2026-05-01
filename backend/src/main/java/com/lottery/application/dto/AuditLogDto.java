package com.lottery.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID actorUserId,
        List<String> actorRoleCodes,
        String action,
        String entityType,
        UUID entityId,
        String requestId,
        String ipAddress,
        String userAgent,
        String beforeJson,
        String afterJson,
        Instant createdAt) {
    public AuditLogDto {
        actorRoleCodes = List.copyOf(actorRoleCodes);
    }
}
