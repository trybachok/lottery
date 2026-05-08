package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lottery.application.mapper.DrawResultMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.draw.GetDrawResultUseCase;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GetDrawResultUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void returnsDrawResultDtoWithRequestContextIds() {
        UUID drawId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();
        Draw draw = new Draw(
                drawId,
                "May draw",
                "",
                DrawStatus.COMPLETED,
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
                NOW,
                null,
                0);
        DrawResult result = new DrawResult(
                UUID.randomUUID(),
                drawId,
                new Combination(List.of("1", "2")),
                "test",
                "test-random",
                "proof",
                UUID.randomUUID(),
                NOW,
                "req_saved",
                "corr_saved");
        GetDrawResultUseCase useCase = new GetDrawResultUseCase(
                new SingleDrawRepository(draw),
                new SingleDrawResultRepository(result),
                grantAll(),
                directTransaction(),
                new DrawResultMapper());

        var dto = useCase.execute(drawId, new UseCaseContext(
                UUID.randomUUID(),
                Set.of(PermissionCodes.DRAW_RESULT_READ),
                "req_current"));

        assertEquals(drawId, dto.drawId());
        assertEquals(List.of("1", "2"), dto.winningCombinationValues());
        assertEquals("req_saved", dto.requestId());
        assertEquals("corr_saved", dto.correlationId());
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

    private record SingleDrawRepository(Draw draw) implements DrawRepository {
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

    private record SingleDrawResultRepository(DrawResult result) implements DrawResultRepository {
        @Override
        public DrawResult save(DrawResult drawResult) {
            return drawResult;
        }

        @Override
        public Optional<DrawResult> findByDrawId(UUID drawId) {
            return result.drawId().equals(drawId) ? Optional.of(result) : Optional.empty();
        }

        @Override
        public boolean existsByDrawId(UUID drawId) {
            return findByDrawId(drawId).isPresent();
        }
    }
}
