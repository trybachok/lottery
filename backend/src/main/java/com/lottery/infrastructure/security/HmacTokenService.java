package com.lottery.infrastructure.security;

import com.lottery.application.UnauthorizedException;
import com.lottery.application.port.auth.TokenIssuerPort;
import com.lottery.application.port.auth.TokenVerifierPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacTokenService implements TokenIssuerPort, TokenVerifierPort {
    private static final String VERSION = "v1";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] secret;
    private final long ttlSeconds;

    public HmacTokenService(String secret, long ttlSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Access token secret must contain at least 32 characters");
        }
        if (ttlSeconds < 60) {
            throw new IllegalArgumentException("Token TTL must be at least 60 seconds");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public IssuedToken issue(UUID userId) {
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        byte[] nonceBytes = new byte[16];
        secureRandom.nextBytes(nonceBytes);
        String nonce = ENCODER.encodeToString(nonceBytes);
        String payload = String.join(".", VERSION, userId.toString(), Long.toString(expiresAt.getEpochSecond()), nonce);
        return new IssuedToken(payload + "." + sign(payload), expiresAt);
    }

    @Override
    public AuthenticatedPrincipal verify(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new UnauthorizedException("Bearer token is required");
        }
        String[] parts = bearerToken.split("\\.");
        if (parts.length != 5 || !VERSION.equals(parts[0])) {
            throw new UnauthorizedException("Invalid bearer token");
        }
        String payload = String.join(".", parts[0], parts[1], parts[2], parts[3]);
        if (!constantTimeEquals(sign(payload), parts[4])) {
            throw new UnauthorizedException("Invalid bearer token");
        }
        Instant expiresAt;
        UUID userId;
        try {
            userId = UUID.fromString(parts[1]);
            expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[2]));
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("Invalid bearer token");
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new UnauthorizedException("Bearer token expired");
        }
        return new AuthenticatedPrincipal(userId, Set.of());
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign access token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
