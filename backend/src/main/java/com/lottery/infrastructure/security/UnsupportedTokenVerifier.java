package com.lottery.infrastructure.security;

import com.lottery.application.ApplicationException;
import com.lottery.application.port.auth.TokenVerifierPort;

public final class UnsupportedTokenVerifier implements TokenVerifierPort {
    @Override
    public AuthenticatedPrincipal verify(String bearerToken) {
        throw new ApplicationException("AUTH_NOT_CONFIGURED", "Token verification adapter is not configured");
    }
}
