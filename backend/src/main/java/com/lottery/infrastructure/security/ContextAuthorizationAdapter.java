package com.lottery.infrastructure.security;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;

public final class ContextAuthorizationAdapter implements AuthorizationPort {
    @Override
    public void ensurePermission(UseCaseContext context, String permissionCode) {
        if (context == null || (!context.permissions().contains(permissionCode) && !context.permissions().contains("*"))) {
            throw new ForbiddenException(permissionCode);
        }
    }
}
