package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationEvaluatorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.draw.RunDrawUseCase;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.model.WinningRule;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RunDrawUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void completesDrawAndMarksPaidTicketsWinOrLose() {
        TestFixture fixture = new TestFixture();
        UUID drawId = fixture.draw.id();

        var result = fixture.useCase.execute(drawId, context());

        assertEquals(drawId, result.drawId());
        assertEquals(2, result.processedTickets());
        assertEquals(1, result.winningTickets());
        assertEquals(1, result.losingTickets());
        assertEquals(DrawStatus.COMPLETED, fixture.draws.findById(drawId).orElseThrow().status());
        assertEquals(TicketStatus.WIN, fixture.tickets.byId.get(fixture.winningTicket.id()).status());
        assertEquals(TicketStatus.LOSE, fixture.tickets.byId.get(fixture.losingTicket.id()).status());
        assertEquals(BigDecimal.valueOf(100).setScale(2), fixture.tickets.byId.get(fixture.winningTicket.id()).matchPercent().orElseThrow());
    }

    @Test
    void rejectsSecondExecutionWhenResultAlreadyExists() {
        TestFixture fixture = new TestFixture();
        fixture.useCase.execute(fixture.draw.id(), context());

        assertThrows(ConflictException.class, () -> fixture.useCase.execute(fixture.draw.id(), context()));
    }

    private static UseCaseContext context() {
        return new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.DRAW_RUN), "req_test");
    }

    private static final class TestFixture {
        private final UUID schemaId = UUID.randomUUID();
        private final Draw draw = new Draw(
                UUID.randomUUID(),
                "May draw",
                "",
                DrawStatus.SALES_CLOSED,
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
        private final Ticket winningTicket = paidTicket(new Combination(List.of("1", "2")));
        private final Ticket losingTicket = paidTicket(new Combination(List.of("9", "9")));
        private final InMemoryDrawRepository draws = new InMemoryDrawRepository(draw);
        private final InMemoryTicketRepository tickets = new InMemoryTicketRepository(List.of(winningTicket, losingTicket));
        private final InMemoryDrawResultRepository results = new InMemoryDrawResultRepository();
        private final RunDrawUseCase useCase = new RunDrawUseCase(
                draws,
                id -> Optional.of(new CombinationSchema(
                        schemaId,
                        "numbers",
                        new CombinationSchemaDefinition("{\"positions\":[{\"type\":\"NUMBER\"},{\"type\":\"NUMBER\"}],\"orderSensitive\":true}"),
                        NOW)),
                results,
                tickets,
                drawId -> List.of(new WinningRule(
                        UUID.randomUUID(),
                        drawId,
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(100),
                        UUID.randomUUID(),
                        0)),
                grantAll(),
                directTransaction(),
                schema -> new WinningCombinationGeneratorPort.GeneratedWinningCombination(
                        new Combination(List.of("1", "2")),
                        "test",
                        "test",
                        "proof"),
                evaluator(),
                new DrawStatusTransitionPolicy(),
                fixedClock());

        private Ticket paidTicket(Combination combination) {
            return new Ticket(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    draw.id(),
                    TicketStatus.PAID,
                    combination,
                    new Money(BigDecimal.TEN, Currency.getInstance("RUB")),
                    null,
                    null,
                    false,
                    NOW.minusSeconds(120),
                    NOW.minusSeconds(100),
                    null,
                    null,
                    null,
                    0);
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

    private static CombinationEvaluatorPort evaluator() {
        return (ticketCombination, winningCombination, schema) -> ticketCombination.values().equals(winningCombination.values())
                ? BigDecimal.valueOf(100).setScale(2)
                : BigDecimal.ZERO.setScale(2);
    }

    private static DomainClock fixedClock() {
        return () -> NOW;
    }

    private static final class InMemoryDrawRepository implements DrawRepository {
        private final Map<UUID, Draw> byId = new HashMap<>();

        private InMemoryDrawRepository(Draw draw) {
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

    private static final class InMemoryTicketRepository implements TicketRepository {
        private final Map<UUID, Ticket> byId = new HashMap<>();

        private InMemoryTicketRepository(List<Ticket> tickets) {
            for (Ticket ticket : tickets) {
                byId.put(ticket.id(), ticket);
            }
        }

        @Override
        public Ticket save(Ticket ticket) {
            byId.put(ticket.id(), ticket);
            return ticket;
        }

        @Override
        public Ticket update(Ticket ticket) {
            Ticket updated = new Ticket(
                    ticket.id(),
                    ticket.userId(),
                    ticket.drawId(),
                    ticket.status(),
                    ticket.combination(),
                    ticket.price(),
                    ticket.matchPercent().orElse(null),
                    ticket.prizeId().orElse(null),
                    ticket.test(),
                    ticket.createdAt(),
                    ticket.paidAt().orElse(null),
                    ticket.checkedAt().orElse(null),
                    ticket.cancelledAt().orElse(null),
                    ticket.deletedAt().orElse(null),
                    ticket.version() + 1);
            byId.put(updated.id(), updated);
            return updated;
        }

        @Override
        public Optional<Ticket> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Ticket> findAll(int limit, int offset) {
            return byId.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<Ticket> findByUserId(UUID userId, int limit, int offset) {
            return byId.values().stream()
                    .filter(ticket -> ticket.userId().equals(userId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<Ticket> findPaidByDrawId(UUID drawId) {
            return byId.values().stream()
                    .filter(ticket -> ticket.drawId().equals(drawId))
                    .filter(ticket -> ticket.status() == TicketStatus.PAID)
                    .toList();
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
