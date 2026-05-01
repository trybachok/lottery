package com.lottery.presentation.rest;

import com.lottery.application.UseCaseContext;
import com.lottery.presentation.middleware.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ServletUseCaseContextFactory {
    public UseCaseContext from(HttpServletRequest request) {
        UUID actorUserId = optionalUuid(request.getHeader("X-Actor-User-Id"));
        Set<String> permissions = permissions(request.getHeader("X-Permissions"));
        return new UseCaseContext(actorUserId, permissions, requestId(request));
    }

    private UUID optionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private Set<String> permissions(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContext.ATTRIBUTE_NAME);
        if (value instanceof RequestContext requestContext) {
            return requestContext.requestId();
        }
        return "req_unknown";
    }
}
