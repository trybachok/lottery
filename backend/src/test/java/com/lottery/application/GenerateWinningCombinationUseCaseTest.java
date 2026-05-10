package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.mapper.DrawResultMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.draw.GenerateWinningCombinationUseCase;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GenerateWinningCombinationUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void generatesResultAndMovesDrawToDrawing() {
        TestFixture fixture = new TestFixture(DrawStatus.SALES_CLOSED);

        var result = fixture.useCase.execute(fixture.draw.id(), context());

        assertEquals(fixture.draw.id(), result.drawId());
        assertEquals(List.of("7", "9"), result.winningCombinationValues());
        assertEquals("secure-test-v1", result.algorithmVersion());
        assertEquals("SecureRandomTest", result.randomProvider());
        assertEquals("proof_hash", result.proofHash());
        assertEquals(DrawStatus.DRAWING, fixture.draws.findById(fixture.draw.id()).orElseThrow().status());
        assertEquals(1, fixture.results.results.size());
    }

    @Test
    void rejectsSecondGeneration() {
        TestFixture fixture = new TestFixture(DrawStatus.SALES_CLOSED);
        fixture.useCase.execute(fixture.draw.id(), context());

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> fixture.useCase.execute(fixture.draw.id(), context()));

        assertEquals("DRAW_RESULT_ALREADY_EXISTS", exception.code());
    }

    @Test
    void rejectsGenerationWhenDrawIsNotSalesClosed() {
        TestFixture fixture = new TestFixture(DrawStatus.ACTIVE);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> fixture.useCase.execute(fixture.draw.id(), context()));

        assertEquals("DRAW_NOT_READY", exception.code());
    }

    @Test
    void mapsInvalidSchemaGenerationToValidationError() {
        TestFixture fixture = new TestFixture(DrawStatus.SALES_CLOSED, true);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> fixture.useCase.execute(fixture.draw.id(), context()));

        assertEquals("VALIDATION_ERROR", exception.code());
        assertEquals(DrawStatus.SALES_CLOSED, fixture.draws.findById(fixture.draw.id()).orElseThrow().status());
        assertEquals(0, fixture.results.results.size());
    }

    private static UseCaseContext context() {
        return new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.DRAW_RUN), "req_test");
    }

    private static final class TestFixture {
        private final UUID schemaId = UUID.randomUUID();
        private final Draw draw;
        private final InMemoryDrawRepository draws;
        private final InMemoryDrawResultRepository results = new InMemoryDrawResultRepository();
        private final GenerateWinningCombinationUseCase useCase;

        private TestFixture(DrawStatus status) {
            this(status, false);
        }

        private TestFixture(DrawStatus status, boolean failGeneration) {
            draw = new Draw(
                    UUID.randomUUID(),
                    "May draw",
                    "",
                    status,
                    null,
                    schemaId,
                    null,
                    null,
                    NOW.minusSeconds(3600),
                    NOW.minusSeconds(60),
                    NOW,
                    null,
                    false,
                    NOW.minusSeconds(7200),
                    NOW.minusSeconds(60),
                    null,
                    0);
            draws = new InMemoryDrawRepository(draw);
            CombinationSchemaRepository schemas = id -> Optional.of(new CombinationSchema(
                    schemaId,
                    "numbers",
                    new CombinationSchemaDefinition(
                            "{\"positions\":[{\"type\":\"NUMBER\"},{\"type\":\"NUMBER\"}],\"orderSensitive\":true}"),
                    NOW));
            useCase = new GenerateWinningCombinationUseCase(
                    draws,
                    schemas,
                    results,
                    grantAll(),
                    directTransaction(),
                    schema -> {
                        if (failGeneration) {
                            throw new IllegalArgumentException("Invalid schema");
                        }
                        return new com.lottery.application.port.lottery.WinningCombinationGeneratorPort.GeneratedWinningCombination(
                                new Combination(List.of("7", "9")),
                                "secure-test-v1",
                                "SecureRandomTest",
                                "proof_hash");
                    },
                    (combination, schema) -> {
                    },
                    new DrawStatusTransitionPolicy(),
                    () -> NOW,
                    new DrawResultMapper(),
                    null);
        }
    }

    private static AuthorizationPort grantAll() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return false;
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

    private static final class InMemoryDrawRepository implements DrawRepository {
        private Draw draw;

        private InMemoryDrawRepository(Draw draw) {
            this.draw = draw;
        }

        @Override
        public Draw save(Draw draw) {
            this.draw = draw;
            return draw;
        }

        @Override
        public Draw update(Draw draw) {
            this.draw = new Draw(
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
            return this.draw;
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

    private static final class InMemoryDrawResultRepository implements DrawResultRepository {
        private final List<DrawResult> results = new ArrayList<>();

        @Override
        public DrawResult save(DrawResult drawResult) {
            results.add(drawResult);
            return drawResult;
        }

        @Override
        public Optional<DrawResult> findByDrawId(UUID drawId) {
            return results.stream().filter(result -> result.drawId().equals(drawId)).findFirst();
        }

        @Override
        public boolean existsByDrawId(UUID drawId) {
            return findByDrawId(drawId).isPresent();
        }
    }
}
