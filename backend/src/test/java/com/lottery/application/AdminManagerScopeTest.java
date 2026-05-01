package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.audit.AuditService;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.ListDrawsQuery;
import com.lottery.application.usecase.draw.AssignDrawManagerUseCase;
import com.lottery.application.usecase.draw.ListDrawsUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.domain.valueobject.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AdminManagerScopeTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void managerListsOnlyAssignedDraws() {
        UUID managerId = UUID.randomUUID();
        Draw own = draw(managerId);
        Draw other = draw(UUID.randomUUID());
        ListDrawsUseCase useCase = new ListDrawsUseCase(
                new Draws(List.of(own, other)), auth(Set.of(RoleCodes.MANAGER)), tx(), new DrawMapper());

        var result = useCase.execute(
                new ListDrawsQuery(20, 0),
                new UseCaseContext(managerId, Set.of(PermissionCodes.DRAW_READ), "req", Set.of(RoleCodes.MANAGER), null, null));

        assertEquals(List.of(own.id()), result.stream().map(item -> item.id()).toList());
    }

    @Test
    void assignDrawManagerRejectsUserWithoutManagerRole() {
        UUID userId = UUID.randomUUID();
        Draw draw = draw(null);
        Rbac rbac = new Rbac(Map.of(userId, Set.of(RoleCodes.CLIENT)));
        AssignDrawManagerUseCase useCase = new AssignDrawManagerUseCase(
                new Draws(List.of(draw)),
                new Users(Map.of(userId, user(userId))),
                rbac,
                auth(Set.of(RoleCodes.ADMIN)),
                tx(),
                clock(),
                new DrawMapper(),
                new AuditService(new AuditLogs(), rbac, clock()));

        assertThrows(
                ForbiddenException.class,
                () -> useCase.execute(
                        draw.id(),
                        userId,
                        new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.DRAW_UPDATE), "req")));
    }

    private static Draw draw(UUID managerId) {
        return new Draw(
                UUID.randomUUID(),
                "Draw",
                "",
                DrawStatus.DRAFT,
                managerId,
                UUID.randomUUID(),
                null,
                null,
                NOW,
                NOW.plusSeconds(60),
                NOW.plusSeconds(120),
                null,
                false,
                NOW,
                NOW,
                null,
                0);
    }

    private static User user(UUID id) {
        return new User(id, "u@example.com", "user", "hash", UserStatus.ACTIVE, NOW, NOW, null, 0);
    }

    private static AuthorizationPort auth(Set<String> roles) {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                if (!context.permissions().contains(permissionCode)) {
                    throw new ForbiddenException(permissionCode);
                }
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return roles.contains(roleCode) || context.actorRoleCodes().contains(roleCode);
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

    private static final class Draws implements DrawRepository {
        private final List<Draw> draws;

        private Draws(List<Draw> draws) {
            this.draws = draws;
        }

        @Override
        public Draw save(Draw draw) {
            return draw;
        }

        @Override
        public Draw update(Draw draw) {
            return draw;
        }

        @Override
        public Optional<Draw> findById(UUID id) {
            return draws.stream().filter(draw -> draw.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Draw> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<Draw> findAll(int limit, int offset) {
            return draws.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<Draw> findReport(UUID drawId, UUID managerId, DrawStatus status, Instant createdFrom, Instant createdTo,
                int limit, int offset) {
            return draws.stream()
                    .filter(draw -> managerId == null || draw.managerId().orElse(null).equals(managerId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class Users implements UserRepository {
        private final Map<UUID, User> users;

        private Users(Map<UUID, User> users) {
            this.users = users;
        }

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public User update(User user) {
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmailOrLogin(String loginOrEmail) {
            return Optional.empty();
        }

        @Override
        public List<User> findAll(int limit, int offset) {
            return users.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }

        @Override
        public boolean existsByLogin(String login) {
            return false;
        }
    }

    private static final class Rbac implements RbacRepository {
        private final Map<UUID, Set<String>> roles;

        private Rbac(Map<UUID, Set<String>> roles) {
            this.roles = roles;
        }

        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            return roles.getOrDefault(userId, Set.of());
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
        }
    }

    private static final class AuditLogs implements AuditLogRepository {
        @Override
        public void append(AuditLog auditLog) {
        }

        @Override
        public List<AuditLog> find(UUID actorUserId, String action, String entityType, UUID entityId, Instant createdFrom,
                Instant createdTo, int limit, int offset) {
            return List.of();
        }
    }
}
