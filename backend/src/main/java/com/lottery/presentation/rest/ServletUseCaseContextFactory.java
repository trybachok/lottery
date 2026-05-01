package com.lottery.presentation.rest;

import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.TokenVerifierPort;
import com.lottery.presentation.middleware.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

public final class ServletUseCaseContextFactory {
    private final TokenVerifierPort tokenVerifierPort;

    public ServletUseCaseContextFactory(TokenVerifierPort tokenVerifierPort) {
        this.tokenVerifierPort = tokenVerifierPort;
    }

    public UseCaseContext from(HttpServletRequest request) {
        String token = bearerToken(request);
        TokenVerifierPort.AuthenticatedPrincipal principal = tokenVerifierPort.verify(token);
        return new UseCaseContext(principal.userId(), Set.of(), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContext.ATTRIBUTE_NAME);
        if (value instanceof RequestContext requestContext) {
            return requestContext.requestId();
        }
        return "req_unknown";
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
