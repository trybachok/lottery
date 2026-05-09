package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lottery.application.audit.AuditService;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.admin.AdminRbacUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.domain.valueobject.UserStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AdminRbacUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");
    private static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOM_ROLE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_READ_PERMISSION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOM_PERMISSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void rejectsDeletingSystemRole() {
        Fixture fixture = new Fixture();

        assertThrows(ValidationException.class, () -> fixture.useCase.deleteRole(ADMIN_ROLE_ID, adminContext(UUID.randomUUID())));
    }

    @Test
    void rejectsDeletingSystemPermission() {
        Fixture fixture = new Fixture();

        assertThrows(
                ValidationException.class,
                () -> fixture.useCase.deletePermission(USER_READ_PERMISSION_ID, adminContext(UUID.randomUUID())));
    }

    @Test
    void rejectsRemovingOwnAdminRole() {
        Fixture fixture = new Fixture();
        UUID actorId = UUID.randomUUID();
        fixture.users.save(user(actorId, "admin@example.com", "admin"));
        fixture.rbac.userRoleIds.put(actorId, new HashSet<>(Set.of(ADMIN_ROLE_ID)));

        assertThrows(
                ValidationException.class,
                () -> fixture.useCase.removeUserRole(actorId, ADMIN_ROLE_ID, adminContext(actorId)));
    }

    @Test
    void rejectsBlockingOwnAdminAccount() {
        Fixture fixture = new Fixture();
        UUID actorId = UUID.randomUUID();
        fixture.users.save(user(actorId, "admin@example.com", "admin"));
        fixture.rbac.userRoleIds.put(actorId, new HashSet<>(Set.of(ADMIN_ROLE_ID)));

        assertThrows(
                ValidationException.class,
                () -> fixture.useCase.updateUser(
                        actorId,
                        "admin@example.com",
                        "admin",
                        null,
                        UserStatus.BLOCKED.name(),
                        adminContext(actorId)));
    }

    @Test
    void recordsBeforeAfterForMutatingAdminAction() {
        Fixture fixture = new Fixture();

        fixture.useCase.updateRole(
                CUSTOM_ROLE_ID,
                "SUPPORT",
                "Support desk",
                "Updated support role",
                adminContext(UUID.randomUUID()));

        AuditLog auditLog = fixture.auditLogs.items.getFirst();
        assertEquals("ADMIN_ROLE_UPDATE", auditLog.action());
        assertNotNull(auditLog.beforeJson());
        assertNotNull(auditLog.afterJson());
        assertTrue(auditLog.beforeJson().contains("\"name\":\"Support\""));
        assertTrue(auditLog.afterJson().contains("\"name\":\"Support desk\""));
    }

    @Test
    void recordsBeforeAfterForRolePermissionAssignment() {
        Fixture fixture = new Fixture();

        fixture.useCase.assignRolePermission(CUSTOM_ROLE_ID, CUSTOM_PERMISSION_ID, adminContext(UUID.randomUUID()));

        AuditLog auditLog = fixture.auditLogs.items.getFirst();
        assertEquals("ADMIN_ROLE_PERMISSION_ASSIGN", auditLog.action());
        assertTrue(auditLog.beforeJson().contains("\"permissions\":[]"));
        assertTrue(auditLog.afterJson().contains("support.ticket.read"));
    }

    private static UseCaseContext adminContext(UUID actorId) {
        return new UseCaseContext(
                actorId,
                Set.of(
                        PermissionCodes.USER_READ,
                        PermissionCodes.USER_CREATE,
                        PermissionCodes.USER_UPDATE,
                        PermissionCodes.USER_DELETE,
                        PermissionCodes.ROLE_READ,
                        PermissionCodes.ROLE_MANAGE,
                        PermissionCodes.PERMISSION_MANAGE),
                "req_test",
                Set.of(RoleCodes.ADMIN),
                "127.0.0.1",
                "test");
    }

    private static User user(UUID id, String email, String login) {
        return new User(id, email, login, "hash", UserStatus.ACTIVE, NOW, NOW, null, 0);
    }

    private static AuthorizationPort auth() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                if (!context.permissions().contains(permissionCode)) {
                    throw new ForbiddenException(permissionCode);
                }
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return context.actorRoleCodes().contains(roleCode);
            }
        };
    }

    private static PasswordHasher passwordHasher() {
        return new PasswordHasher() {
            @Override
            public String hash(String rawPassword) {
                return "hash:" + rawPassword;
            }

            @Override
            public boolean verify(String rawPassword, String passwordHash) {
                return ("hash:" + rawPassword).equals(passwordHash);
            }
        };
    }

    private static TransactionManager tx() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionalWork<T> work) {
                return work.execute();
            }
        };
    }

    private static DomainClock clock() {
        return () -> NOW;
    }

    private static final class Fixture {
        private final Users users = new Users();
        private final Rbac rbac = new Rbac();
        private final AuditLogs auditLogs = new AuditLogs();
        private final AdminRbacUseCase useCase = new AdminRbacUseCase(
                users,
                rbac,
                auth(),
                passwordHasher(),
                tx(),
                clock(),
                new UserMapper(),
                new AuditService(auditLogs, rbac, clock()));

        private Fixture() {
            rbac.roles.put(ADMIN_ROLE_ID, new Role(ADMIN_ROLE_ID, RoleCodes.ADMIN, "Administrator", "Full access", true));
            rbac.roles.put(CUSTOM_ROLE_ID, new Role(CUSTOM_ROLE_ID, "SUPPORT", "Support", "Support role", false));
            rbac.permissions.put(
                    USER_READ_PERMISSION_ID,
                    new Permission(USER_READ_PERMISSION_ID, PermissionCodes.USER_READ, "Read users"));
            rbac.permissions.put(
                    CUSTOM_PERMISSION_ID,
                    new Permission(CUSTOM_PERMISSION_ID, "support.ticket.read", "Read support tickets"));
        }
    }

    private static final class Users implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();

        @Override
        public User save(User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public User update(User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmailOrLogin(String loginOrEmail) {
            return users.values().stream()
                    .filter(user -> user.email().equals(loginOrEmail) || user.login().equals(loginOrEmail))
                    .findFirst();
        }

        @Override
        public List<User> findAll(int limit, int offset) {
            return users.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(user -> user.email().equals(email));
        }

        @Override
        public boolean existsByLogin(String login) {
            return users.values().stream().anyMatch(user -> user.login().equals(login));
        }

        @Override
        public boolean existsByEmailExceptId(String email, UUID id) {
            return users.values().stream().anyMatch(user -> !user.id().equals(id) && user.email().equals(email));
        }

        @Override
        public boolean existsByLoginExceptId(String login, UUID id) {
            return users.values().stream().anyMatch(user -> !user.id().equals(id) && user.login().equals(login));
        }
    }

    private static final class Rbac implements RbacRepository {
        private final Map<UUID, Role> roles = new HashMap<>();
        private final Map<UUID, Permission> permissions = new HashMap<>();
        private final Map<UUID, Set<UUID>> userRoleIds = new HashMap<>();
        private final Map<UUID, Set<UUID>> rolePermissionIds = new HashMap<>();

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            Set<String> permissionCodes = new HashSet<>();
            for (UUID roleId : userRoleIds.getOrDefault(userId, Set.of())) {
                for (UUID permissionId : rolePermissionIds.getOrDefault(roleId, Set.of())) {
                    permissionCodes.add(permissions.get(permissionId).code());
                }
            }
            return permissionCodes;
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            Set<String> roleCodes = new HashSet<>();
            for (UUID roleId : userRoleIds.getOrDefault(userId, Set.of())) {
                roleCodes.add(roles.get(roleId).code());
            }
            return roleCodes;
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
            roles.values().stream()
                    .filter(role -> role.code().equals(roleCode))
                    .findFirst()
                    .ifPresent(role -> assignRole(userId, role.id()));
        }

        @Override
        public List<Role> findAllRoles(int limit, int offset) {
            return roles.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public Optional<Role> findRoleById(UUID id) {
            return Optional.ofNullable(roles.get(id));
        }

        @Override
        public Role saveRole(Role role) {
            roles.put(role.id(), role);
            return role;
        }

        @Override
        public Role updateRole(Role role) {
            roles.put(role.id(), role);
            return role;
        }

        @Override
        public void deleteRole(UUID id) {
            roles.remove(id);
        }

        @Override
        public List<Permission> findAllPermissions(int limit, int offset) {
            return permissions.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public Optional<Permission> findPermissionById(UUID id) {
            return Optional.ofNullable(permissions.get(id));
        }

        @Override
        public Permission savePermission(Permission permission) {
            permissions.put(permission.id(), permission);
            return permission;
        }

        @Override
        public Permission updatePermission(Permission permission) {
            permissions.put(permission.id(), permission);
            return permission;
        }

        @Override
        public void deletePermission(UUID id) {
            permissions.remove(id);
        }

        @Override
        public List<Role> findRolesByUserId(UUID userId) {
            return userRoleIds.getOrDefault(userId, Set.of()).stream().map(roles::get).toList();
        }

        @Override
        public void assignRole(UUID userId, UUID roleId) {
            userRoleIds.computeIfAbsent(userId, ignored -> new HashSet<>()).add(roleId);
        }

        @Override
        public void removeRole(UUID userId, UUID roleId) {
            userRoleIds.computeIfAbsent(userId, ignored -> new HashSet<>()).remove(roleId);
        }

        @Override
        public List<Permission> findPermissionsByRoleId(UUID roleId) {
            return rolePermissionIds.getOrDefault(roleId, Set.of()).stream().map(permissions::get).toList();
        }

        @Override
        public void assignPermission(UUID roleId, UUID permissionId) {
            rolePermissionIds.computeIfAbsent(roleId, ignored -> new HashSet<>()).add(permissionId);
        }

        @Override
        public void removePermission(UUID roleId, UUID permissionId) {
            rolePermissionIds.computeIfAbsent(roleId, ignored -> new HashSet<>()).remove(permissionId);
        }
    }

    private static final class AuditLogs implements AuditLogRepository {
        private final List<AuditLog> items = new ArrayList<>();

        @Override
        public void append(AuditLog auditLog) {
            items.add(auditLog);
        }

        @Override
        public List<AuditLog> find(
                UUID actorUserId,
                String action,
                String entityType,
                UUID entityId,
                Instant createdFrom,
                Instant createdTo,
                int limit,
                int offset) {
            return items.stream().skip(offset).limit(limit).toList();
        }
    }
}
