package com.lottery.application.port.auth;

import java.time.Instant;
import java.util.UUID;

public interface TokenIssuerPort {
    IssuedToken issue(UUID userId);

    record IssuedToken(String value, Instant expiresAt) {
    }
}
