package com.lottery.application;

import java.util.Set;
import java.util.UUID;

public record UseCaseContext(UUID actorUserId, Set<String> permissions, String requestId) {
    public UseCaseContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
