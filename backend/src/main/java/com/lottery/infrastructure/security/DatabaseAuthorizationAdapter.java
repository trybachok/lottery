package com.lottery.infrastructure.security;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UnauthorizedException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.valueobject.RoleCodes;

public final class DatabaseAuthorizationAdapter implements AuthorizationPort {
    private final RbacRepository rbacRepository;

    public DatabaseAuthorizationAdapter(RbacRepository rbacRepository) {
        this.rbacRepository = rbacRepository;
    }

    @Override
    public void ensurePermission(UseCaseContext context, String permissionCode) {
        if (context == null || context.actorUserId() == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        if (hasRole(context, RoleCodes.ADMIN)) {
            return;
        }
        if (context.permissions().contains(permissionCode)) {
            return;
        }
        if (context.permissions().isEmpty()
                && rbacRepository.findPermissionCodesByUserId(context.actorUserId()).contains(permissionCode)) {
            return;
        }
        throw new ForbiddenException(permissionCode);
    }

    @Override
    public boolean hasRole(UseCaseContext context, String roleCode) {
        if (context == null || context.actorUserId() == null) {
            return false;
        }
        if (context.actorRoleCodes().contains(roleCode)) {
            return true;
        }
        if (!context.actorRoleCodes().isEmpty()) {
            return false;
        }
        return rbacRepository.findRoleCodesByUserId(context.actorUserId()).contains(roleCode);
    }
}
