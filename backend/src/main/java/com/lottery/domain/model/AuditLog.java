package com.lottery.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record AuditLog(
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
    public AuditLog {
        Objects.requireNonNull(id, "id");
        actorRoleCodes = List.copyOf(Objects.requireNonNull(actorRoleCodes, "actorRoleCodes"));
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public Optional<UUID> maybeActorUserId() {
        return Optional.ofNullable(actorUserId);
    }
}
