package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.command.CreateCombinationSchemaCommand;
import com.lottery.application.command.CreateDrawCommand;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationSchemaValidatorPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CreateDrawUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant SALES_START = NOW.plusSeconds(60);
    private static final Instant SALES_END = NOW.plusSeconds(120);
    private static final Instant DRAW_AT = NOW.plusSeconds(180);

    @Test
    void createsCombinationSchemaAndDrawInOneUseCase() {
        Draws draws = new Draws();
        Schemas schemas = new Schemas();
        CreateDrawUseCase useCase = useCase(draws, schemas, validatingSchema -> {
        }, auth());

        var result = useCase.execute(command(null, inlineSchema()), adminContext());

        assertNotNull(result.id());
        assertEquals(1, schemas.saved.size());
        assertEquals(schemas.saved.getFirst().id(), result.combinationSchemaId());
        assertEquals(1, draws.saved.size());
        assertEquals(result.combinationSchemaId(), draws.saved.getFirst().combinationSchemaId());
    }

    @Test
    void managerCreatesDrawAssignedToSelfWhenManagerIdIsEmpty() {
        UUID managerId = UUID.randomUUID();
        Draws draws = new Draws();
        CreateDrawUseCase useCase = useCase(draws, new Schemas(), validatingSchema -> {
        }, auth());

        var result = useCase.execute(command(null, inlineSchema()), managerContext(managerId));

        assertEquals(managerId, result.managerId());
        assertEquals(managerId, draws.saved.getFirst().managerId().orElseThrow());
    }

    @Test
    void managerCannotCreateDrawForAnotherManager() {
        UUID managerId = UUID.randomUUID();
        UUID otherManagerId = UUID.randomUUID();
        CreateDrawUseCase useCase = useCase(new Draws(), new Schemas(), validatingSchema -> {
        }, auth());

        assertThrows(ForbiddenException.class, () -> useCase.execute(command(otherManagerId, inlineSchema()), managerContext(managerId)));
    }

    @Test
    void rejectsInvalidInlineCombinationSchema() {
        Schemas schemas = new Schemas();
        CreateDrawUseCase useCase = useCase(
                new Draws(),
                schemas,
                validatingSchema -> {
                    throw new IllegalArgumentException("positions must be a non-empty array");
                },
                auth());

        assertThrows(ValidationException.class, () -> useCase.execute(command(null, inlineSchema()), adminContext()));
        assertEquals(0, schemas.saved.size());
    }

    private static CreateDrawCommand command(UUID managerId, CreateCombinationSchemaCommand schema) {
        return new CreateDrawCommand(
                "May draw",
                "",
                managerId,
                null,
                schema,
                SALES_START,
                SALES_END,
                DRAW_AT,
                null,
                false);
    }

    private static CreateCombinationSchemaCommand inlineSchema() {
        return new CreateCombinationSchemaCommand(
                "Demo schema",
                """
                {
                  "positions": [
                    { "type": "NUMBER", "min": 7, "max": 7 },
                    { "type": "NUMBER", "min": 1, "max": 2 }
                  ],
                  "allowDuplicates": false,
                  "orderSensitive": true
                }
                """);
    }

    private static CreateDrawUseCase useCase(
            Draws draws,
            Schemas schemas,
            CombinationSchemaValidatorPort validator,
            AuthorizationPort authorizationPort) {
        return new CreateDrawUseCase(
                draws,
                schemas,
                authorizationPort,
                tx(),
                validator,
                clock(),
                new DrawMapper(),
                null);
    }

    private static UseCaseContext adminContext() {
        return new UseCaseContext(
                UUID.randomUUID(),
                Set.of(PermissionCodes.DRAW_CREATE),
                "req",
                Set.of(RoleCodes.ADMIN),
                null,
                null);
    }

    private static UseCaseContext managerContext(UUID managerId) {
        return new UseCaseContext(
                managerId,
                Set.of(PermissionCodes.DRAW_CREATE),
                "req",
                Set.of(RoleCodes.MANAGER),
                null,
                null);
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
        private final List<Draw> saved = new ArrayList<>();

        @Override
        public Draw save(Draw draw) {
            saved.add(draw);
            return draw;
        }

        @Override
        public Draw update(Draw draw) {
            return draw;
        }

        @Override
        public Optional<Draw> findById(UUID id) {
            return saved.stream().filter(draw -> draw.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Draw> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<Draw> findAll(int limit, int offset) {
            return saved.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<Draw> findReport(UUID drawId, UUID managerId, DrawStatus status, Instant createdFrom, Instant createdTo,
                int limit, int offset) {
            return saved.stream().skip(offset).limit(limit).toList();
        }
    }

    private static final class Schemas implements CombinationSchemaRepository {
        private final List<CombinationSchema> saved = new ArrayList<>();

        @Override
        public CombinationSchema save(CombinationSchema schema) {
            saved.add(schema);
            return schema;
        }

        @Override
        public Optional<CombinationSchema> findById(UUID id) {
            return saved.stream().filter(schema -> schema.id().equals(id)).findFirst();
        }
    }
}
