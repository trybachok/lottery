package com.lottery.application.port.auth;

import java.util.Set;
import java.util.UUID;

public interface TokenVerifierPort {
    AuthenticatedPrincipal verify(String bearerToken);

    record AuthenticatedPrincipal(UUID userId, Set<String> permissions) {
        public AuthenticatedPrincipal {
            permissions = Set.copyOf(permissions);
        }
    }
}
