package com.lottery.presentation.rest;

import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.TokenVerifierPort;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.presentation.middleware.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

public final class ServletUseCaseContextFactory {
    private final TokenVerifierPort tokenVerifierPort;
    private final RbacRepository rbacRepository;

    public ServletUseCaseContextFactory(TokenVerifierPort tokenVerifierPort, RbacRepository rbacRepository) {
        this.tokenVerifierPort = tokenVerifierPort;
        this.rbacRepository = rbacRepository;
    }

    public UseCaseContext from(HttpServletRequest request) {
        String token = bearerToken(request);
        TokenVerifierPort.AuthenticatedPrincipal principal = tokenVerifierPort.verify(token);
        return new UseCaseContext(
                principal.userId(),
                rbacRepository.findPermissionCodesByUserId(principal.userId()),
                requestId(request),
                correlationId(request),
                rbacRepository.findRoleCodesByUserId(principal.userId()),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContext.ATTRIBUTE_NAME);
        if (value instanceof RequestContext requestContext) {
            return requestContext.requestId();
        }
        return "req_unknown";
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContext.ATTRIBUTE_NAME);
        if (value instanceof RequestContext requestContext) {
            return requestContext.correlationId();
        }
        return null;
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
