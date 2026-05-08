package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.audit.AuditService;
import com.lottery.application.command.UpdateDrawCommand;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.draw.ChangeDrawStatusUseCase;
import com.lottery.application.usecase.draw.ChangeDrawStatusUseCase.DrawLifecycleAction;
import com.lottery.application.usecase.draw.UpdateDrawUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DrawLifecycleUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void pauseActiveDrawThroughStatusPolicyAndWritesAuditLog() {
        UUID managerId = UUID.randomUUID();
        Draw draw = draw(DrawStatus.ACTIVE, managerId);
        Draws draws = new Draws(draw);
        AuditLogs auditLogs = new AuditLogs();
        ChangeDrawStatusUseCase useCase = new ChangeDrawStatusUseCase(
                draws,
                auth(Set.of(RoleCodes.ADMIN)),
                tx(),
                new DrawStatusTransitionPolicy(),
                clock(),
                new DrawMapper(),
                audit(auditLogs));

        var result = useCase.execute(draw.id(), DrawLifecycleAction.PAUSE, adminContext());

        assertEquals(DrawStatus.PAUSED.name(), result.status());
        assertEquals(DrawStatus.PAUSED, draws.byId.get(draw.id()).status());
        assertEquals(List.of("DRAW_PAUSE"), auditLogs.items.stream().map(AuditLog::action).toList());
    }

    @Test
    void rejectsForbiddenLifecycleTransition() {
        Draw draw = draw(DrawStatus.DRAFT, null);
        ChangeDrawStatusUseCase useCase = new ChangeDrawStatusUseCase(
                new Draws(draw),
                auth(Set.of(RoleCodes.ADMIN)),
                tx(),
                new DrawStatusTransitionPolicy(),
                clock(),
                new DrawMapper(),
                audit(new AuditLogs()));

        assertThrows(
                ConflictException.class,
                () -> useCase.execute(draw.id(), DrawLifecycleAction.PAUSE, adminContext()));
    }

    @Test
    void updateRejectsCompletedDrawWithExistingResult() {
        Draw draw = draw(DrawStatus.COMPLETED, null);
        UpdateDrawUseCase useCase = new UpdateDrawUseCase(
                new Draws(draw),
                new Results(Set.of(draw.id())),
                auth(Set.of(RoleCodes.ADMIN)),
                tx(),
                clock(),
                new DrawMapper(),
                audit(new AuditLogs()));

        assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        new UpdateDrawCommand(draw.id(), "Updated", null, null, null, null, null),
                        adminContext()));
    }

    @Test
    void managerCannotChangeUnassignedDraw() {
        UUID managerId = UUID.randomUUID();
        Draw draw = draw(DrawStatus.ACTIVE, UUID.randomUUID());
        ChangeDrawStatusUseCase useCase = new ChangeDrawStatusUseCase(
                new Draws(draw),
                auth(Set.of(RoleCodes.MANAGER)),
                tx(),
                new DrawStatusTransitionPolicy(),
                clock(),
                new DrawMapper(),
                audit(new AuditLogs()));

        assertThrows(
                ForbiddenException.class,
                () -> useCase.execute(
                        draw.id(),
                        DrawLifecycleAction.PAUSE,
                        new UseCaseContext(managerId, Set.of(PermissionCodes.DRAW_UPDATE), "req", Set.of(RoleCodes.MANAGER), null, null)));
    }

    private static UseCaseContext adminContext() {
        return new UseCaseContext(UUID.randomUUID(), Set.of("*"), "req", Set.of(RoleCodes.ADMIN), null, null);
    }

    private static Draw draw(DrawStatus status, UUID managerId) {
        return new Draw(
                UUID.randomUUID(),
                "Draw",
                "",
                status,
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

    private static AuthorizationPort auth(Set<String> roles) {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
                if (!context.permissions().contains(permissionCode) && !context.permissions().contains("*")) {
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

    private static AuditService audit(AuditLogs auditLogs) {
        return new AuditService(auditLogs, new Rbac(), clock());
    }

    private static final class Draws implements DrawRepository {
        private final Map<UUID, Draw> byId = new HashMap<>();

        private Draws(Draw draw) {
            byId.put(draw.id(), draw);
        }

        @Override
        public Draw save(Draw draw) {
            byId.put(draw.id(), draw);
            return draw;
        }

        @Override
        public Draw update(Draw draw) {
            Draw updated = new Draw(
                    draw.id(),
                    draw.title(),
                    draw.description(),
                    draw.status(),
                    draw.managerId().orElse(null),
                    draw.combinationSchemaId(),
                    draw.uiThemeId().orElse(null),
                    draw.uiTemplateId().orElse(null),
                    draw.salesStartAt(),
                    draw.salesEndAt(),
                    draw.drawAt(),
                    draw.maxTickets().orElse(null),
                    draw.test(),
                    draw.createdAt(),
                    draw.updatedAt(),
                    draw.deletedAt().orElse(null),
                    draw.version() + 1);
            byId.put(updated.id(), updated);
            return updated;
        }

        @Override
        public Optional<Draw> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Draw> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<Draw> findAll(int limit, int offset) {
            return byId.values().stream().skip(offset).limit(limit).toList();
        }
    }

    private static final class Results implements DrawResultRepository {
        private final Set<UUID> drawIds;

        private Results(Set<UUID> drawIds) {
            this.drawIds = drawIds;
        }

        @Override
        public DrawResult save(DrawResult drawResult) {
            drawIds.add(drawResult.drawId());
            return drawResult;
        }

        @Override
        public Optional<DrawResult> findByDrawId(UUID drawId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByDrawId(UUID drawId) {
            return drawIds.contains(drawId);
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
            return items;
        }
    }

    private static final class Rbac implements RbacRepository {
        @Override
        public Set<String> findPermissionCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByUserId(UUID userId) {
            return Set.of();
        }

        @Override
        public void assignRoleByCode(UUID userId, String roleCode) {
        }
    }
}
