package com.lottery.application;

import java.util.Set;
import java.util.UUID;

public record UseCaseContext(
        UUID actorUserId,
        Set<String> permissions,
        String requestId,
        Set<String> actorRoleCodes,
        String ipAddress,
        String userAgent) {
    public UseCaseContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        actorRoleCodes = actorRoleCodes == null ? Set.of() : Set.copyOf(actorRoleCodes);
    }

    public UseCaseContext(UUID actorUserId, Set<String> permissions, String requestId) {
        this(actorUserId, permissions, requestId, Set.of(), null, null);
    }

    public static UseCaseContext anonymous(String requestId) {
        return new UseCaseContext(null, Set.of(), requestId);
    }
}
