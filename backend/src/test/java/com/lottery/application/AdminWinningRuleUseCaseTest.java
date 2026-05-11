package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.audit.AuditService;
import com.lottery.application.command.WinningRuleCommand;
import com.lottery.application.mapper.WinningRuleMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.admin.AdminWinningRuleUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.model.Prize;
import com.lottery.domain.model.WinningRule;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.PrizeRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AdminWinningRuleUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void replacesRulesForEditableDraw() {
        TestFixture fixture = new TestFixture(DrawStatus.ACTIVE);

        var rules = fixture.useCase.replaceRules(
                fixture.draw.id(),
                List.of(new WinningRuleCommand(BigDecimal.valueOf(100), BigDecimal.valueOf(100), fixture.prize.id(), 1)),
                adminContext());

        assertEquals(1, rules.size());
        assertEquals(fixture.draw.id(), rules.getFirst().drawId());
        assertEquals(1, fixture.rules.rules.size());
        assertEquals(1, fixture.auditLogs.size());
    }

    @Test
    void rejectsRulesAfterSalesClosed() {
        TestFixture fixture = new TestFixture(DrawStatus.SALES_CLOSED);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> fixture.useCase.replaceRules(
                        fixture.draw.id(),
                        List.of(new WinningRuleCommand(BigDecimal.valueOf(100), BigDecimal.valueOf(100), fixture.prize.id(), 1)),
                        adminContext()));

        assertEquals("DRAW_NOT_EDITABLE", exception.code());
    }

    @Test
    void rejectsUnassignedManager() {
        TestFixture fixture = new TestFixture(DrawStatus.ACTIVE);

        assertThrows(
                ForbiddenException.class,
                () -> fixture.useCase.replaceRules(
                        fixture.draw.id(),
                        List.of(new WinningRuleCommand(BigDecimal.valueOf(100), BigDecimal.valueOf(100), fixture.prize.id(), 1)),
                        managerContext(UUID.randomUUID())));
    }

    @Test
    void validatesFivePercentStep() {
        TestFixture fixture = new TestFixture(DrawStatus.ACTIVE);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> fixture.useCase.replaceRules(
                        fixture.draw.id(),
                        List.of(new WinningRuleCommand(BigDecimal.valueOf(97), BigDecimal.valueOf(100), fixture.prize.id(), 1)),
                        adminContext()));

        assertEquals("VALIDATION_ERROR", exception.code());
    }

    private static UseCaseContext adminContext() {
        return new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.DRAW_UPDATE), "req_test", Set.of(RoleCodes.ADMIN), null, null);
    }

    private static UseCaseContext managerContext(UUID actorUserId) {
        return new UseCaseContext(actorUserId, Set.of(PermissionCodes.DRAW_UPDATE), "req_test", Set.of(RoleCodes.MANAGER), null, null);
    }

    private static final class TestFixture {
        private final UUID managerId = UUID.randomUUID();
        private final Prize prize = new Prize(
                UUID.randomUUID(),
                "MONEY",
                "Main prize",
                BigDecimal.valueOf(1000),
                Currency.getInstance("RUB"),
                null,
                null,
                null);
        private final Draw draw;
        private final InMemoryWinningRuleRepository rules = new InMemoryWinningRuleRepository();
        private final List<AuditLog> auditLogs = new ArrayList<>();
        private final AdminWinningRuleUseCase useCase;

        private TestFixture(DrawStatus status) {
            draw = new Draw(
                    UUID.randomUUID(),
                    "May draw",
                    "",
                    status,
                    managerId,
                    UUID.randomUUID(),
                    null,
                    null,
                    NOW.minusSeconds(3600),
                    NOW.plusSeconds(3600),
                    NOW.plusSeconds(7200),
                    null,
                    false,
                    NOW.minusSeconds(7200),
                    NOW.minusSeconds(60),
                    null,
                    0);
            useCase = new AdminWinningRuleUseCase(
                    new InMemoryDrawRepository(draw),
                    new EmptyDrawResultRepository(),
                    new InMemoryPrizeRepository(prize),
                    rules,
                    contextAuthorization(),
                    directTransaction(),
                    new WinningRuleMapper(),
                    auditService(auditLogs));
        }
    }

    private static AuthorizationPort contextAuthorization() {
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

    private static TransactionManager directTransaction() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionalWork<T> work) {
                return work.execute();
            }
        };
    }

    private static AuditService auditService(List<AuditLog> auditLogs) {
        return new AuditService(
                new AuditLogRepository() {
                    @Override
                    public void append(AuditLog auditLog) {
                        auditLogs.add(auditLog);
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
                        return List.of();
                    }
                },
                new RbacRepository() {
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
                },
                () -> NOW);
    }

    private static final class InMemoryDrawRepository implements DrawRepository {
        private final Draw draw;

        private InMemoryDrawRepository(Draw draw) {
            this.draw = draw;
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
            return draw.id().equals(id) ? Optional.of(draw) : Optional.empty();
        }

        @Override
        public Optional<Draw> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<Draw> findAll(int limit, int offset) {
            return List.of(draw);
        }
    }

    private static final class EmptyDrawResultRepository implements DrawResultRepository {
        @Override
        public DrawResult save(DrawResult drawResult) {
            return drawResult;
        }

        @Override
        public Optional<DrawResult> findByDrawId(UUID drawId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByDrawId(UUID drawId) {
            return false;
        }
    }

    private static final class InMemoryPrizeRepository implements PrizeRepository {
        private final Prize prize;

        private InMemoryPrizeRepository(Prize prize) {
            this.prize = prize;
        }

        @Override
        public List<Prize> findAll(int limit, int offset) {
            return List.of(prize);
        }

        @Override
        public Optional<Prize> findById(UUID id) {
            return prize.id().equals(id) ? Optional.of(prize) : Optional.empty();
        }

        @Override
        public Prize save(Prize prize) {
            return prize;
        }

        @Override
        public Prize update(Prize prize) {
            return prize;
        }
    }

    private static final class InMemoryWinningRuleRepository implements WinningRuleRepository {
        private final List<WinningRule> rules = new ArrayList<>();

        @Override
        public List<WinningRule> findByDrawIdOrderByPriority(UUID drawId) {
            return rules.stream().filter(rule -> rule.drawId().equals(drawId)).toList();
        }

        @Override
        public WinningRule save(WinningRule winningRule) {
            rules.add(winningRule);
            return winningRule;
        }

        @Override
        public void deleteByDrawId(UUID drawId) {
            rules.removeIf(rule -> rule.drawId().equals(drawId));
        }
    }
}
