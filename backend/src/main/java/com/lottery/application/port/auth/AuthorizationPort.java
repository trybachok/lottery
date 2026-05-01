package com.lottery.application.port.auth;

import com.lottery.application.UseCaseContext;

public interface AuthorizationPort {
    void ensurePermission(UseCaseContext context, String permissionCode);

    boolean hasRole(UseCaseContext context, String roleCode);
}
