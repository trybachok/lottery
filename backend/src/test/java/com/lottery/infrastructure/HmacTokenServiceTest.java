package com.lottery.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.UnauthorizedException;
import com.lottery.infrastructure.security.HmacTokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HmacTokenServiceTest {
    private static final String SECRET = "test-access-token-secret-32-bytes-minimum";

    @Test
    void verifiesIssuedToken() {
        HmacTokenService service = new HmacTokenService(SECRET, 900);
        UUID userId = UUID.randomUUID();

        var token = service.issue(userId);
        var principal = service.verify(token.value());

        assertEquals(userId, principal.userId());
    }

    @Test
    void rejectsTamperedToken() {
        HmacTokenService service = new HmacTokenService(SECRET, 900);
        String token = service.issue(UUID.randomUUID()).value();

        assertThrows(UnauthorizedException.class, () -> service.verify(token + "x"));
    }

    @Test
    void requiresStrongSecret() {
        assertThrows(IllegalArgumentException.class, () -> new HmacTokenService("short", 900));
    }
}
