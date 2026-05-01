package com.lottery.infrastructure.security;

import com.lottery.application.UnauthorizedException;
import com.lottery.application.port.auth.TokenIssuerPort;
import com.lottery.application.port.auth.TokenVerifierPort;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySessionTokenService implements TokenIssuerPort, TokenVerifierPort {
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public InMemorySessionTokenService(long ttlSeconds) {
        if (ttlSeconds < 60) {
            throw new IllegalArgumentException("Token TTL must be at least 60 seconds");
        }
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public IssuedToken issue(UUID userId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        sessions.put(token, new Session(userId, expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public AuthenticatedPrincipal verify(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new UnauthorizedException("Bearer token is required");
        }
        Session session = sessions.get(bearerToken);
        if (session == null) {
            throw new UnauthorizedException("Invalid bearer token");
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(bearerToken);
            throw new UnauthorizedException("Bearer token expired");
        }
        return new AuthenticatedPrincipal(session.userId(), Set.of());
    }

    private record Session(UUID userId, Instant expiresAt) {
    }
}
