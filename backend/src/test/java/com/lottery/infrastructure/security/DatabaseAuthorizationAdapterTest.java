package com.lottery.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UseCaseContext;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DatabaseAuthorizationAdapterTest {
    @Test
    void usesCachedContextPermissionsWithoutRepositoryLookup() {
        DatabaseAuthorizationAdapter adapter = new DatabaseAuthorizationAdapter(new FailingRbac());
        UseCaseContext context = new UseCaseContext(
                UUID.randomUUID(),
                Set.of(PermissionCodes.TICKET_READ),
                "req_test",
                Set.of(RoleCodes.CLIENT),
                null,
                null);

        assertDoesNotThrow(() -> adapter.ensurePermission(context, PermissionCodes.TICKET_READ));
    }

    @Test
    void usesCachedAdminRoleWithoutRepositoryLookup() {
        DatabaseAuthorizationAdapter adapter = new DatabaseAuthorizationAdapter(new FailingRbac());
        UseCaseContext context = new UseCaseContext(
                UUID.randomUUID(),
                Set.of(),
                "req_test",
                Set.of(RoleCodes.ADMIN),
                null,
                null);

        assertDoesNotThrow(() -> adapter.ensurePermission(context, PermissionCodes.SYSTEM_SETTINGS_MANAGE));
    }

    @Test
    void rejectsWhenCachedPermissionsDoNotContainPermission() {
        DatabaseAuthorizationAdapter adapter = new DatabaseAuthorizationAdapter(new FailingRbac());
        UseCaseContext context = new UseCaseContext(
                UUID.randomUUID(),
                Set.of(PermissionCodes.TICKET_READ),
                "req_test",
                Set.of(RoleCodes.CLIENT),
                null,
                null);

        assertThrows(ForbiddenException.class, () -> adapter.ensurePermission(context, PermissionCodes.DRAW_RUN));
    }

    private static final class FailingRbac implements RbacRepository {
        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            throw new AssertionError("RBAC repository should not be called");
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            throw new AssertionError("RBAC repository should not be called");
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findAllRoles(int limit, int offset) {
            return List.of();
        }

        @Override
        public Optional<Role> findRoleById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Permission> findAllPermissions(int limit, int offset) {
            return List.of();
        }
    }
}
