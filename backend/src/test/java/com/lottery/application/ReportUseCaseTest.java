package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lottery.application.audit.AuditService;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.DrawReportQuery;
import com.lottery.application.query.TicketReportQuery;
import com.lottery.application.usecase.report.GenerateDrawReportUseCase;
import com.lottery.application.usecase.report.GenerateTicketReportUseCase;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class ReportUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

    @Test
    void drawReportReturnsPaginationMetadataForFilteredRows() {
        UUID managerId = UUID.randomUUID();
        Draw completedOne = draw(DrawStatus.COMPLETED, managerId, NOW.minusSeconds(30));
        Draw active = draw(DrawStatus.ACTIVE, managerId, NOW.minusSeconds(20));
        Draw completedTwo = draw(DrawStatus.COMPLETED, managerId, NOW.minusSeconds(10));
        AuditLogs auditLogs = new AuditLogs();
        GenerateDrawReportUseCase useCase = new GenerateDrawReportUseCase(
                new Draws(List.of(completedOne, active, completedTwo)),
                auth(),
                tx(),
                new DrawMapper(),
                audit(auditLogs));

        var page = useCase.execute(
                new DrawReportQuery(null, managerId, DrawStatus.COMPLETED, NOW.minusSeconds(60), NOW, 1, 0),
                context(PermissionCodes.REPORT_DRAW_EXPORT));

        assertEquals(1, page.items().size());
        assertEquals(2, page.total());
        assertEquals(1, page.limit());
        assertEquals(0, page.offset());
        assertTrue(page.hasMore());
        assertEquals(List.of("REPORT_DRAW_EXPORT"), auditLogs.items.stream().map(AuditLog::action).toList());
    }

    @Test
    void ticketReportReturnsSecondPageAndTotalForFilteredRows() {
        UUID userId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();
        Ticket paidOne = ticket(userId, drawId, TicketStatus.PAID, NOW.minusSeconds(30));
        Ticket win = ticket(userId, drawId, TicketStatus.WIN, NOW.minusSeconds(20));
        Ticket paidTwo = ticket(userId, drawId, TicketStatus.PAID, NOW.minusSeconds(10));
        GenerateTicketReportUseCase useCase = new GenerateTicketReportUseCase(
                new Tickets(List.of(paidOne, win, paidTwo)),
                auth(),
                tx(),
                new TicketMapper(),
                audit(new AuditLogs()));

        var page = useCase.execute(
                new TicketReportQuery(userId, drawId, TicketStatus.PAID, NOW.minusSeconds(60), NOW, 1, 1),
                context(PermissionCodes.REPORT_TICKET_EXPORT));

        assertEquals(1, page.items().size());
        assertEquals(2, page.total());
        assertEquals(1, page.limit());
        assertEquals(1, page.offset());
        assertEquals(false, page.hasMore());
    }

    private static UseCaseContext context(String permission) {
        return new UseCaseContext(UUID.randomUUID(), Set.of(permission), "req_test");
    }

    private static Draw draw(DrawStatus status, UUID managerId, Instant createdAt) {
        return new Draw(
                UUID.randomUUID(),
                "Draw",
                "",
                status,
                managerId,
                UUID.randomUUID(),
                null,
                null,
                createdAt,
                createdAt.plusSeconds(60),
                createdAt.plusSeconds(120),
                null,
                false,
                createdAt,
                createdAt,
                null,
                0);
    }

    private static Ticket ticket(UUID userId, UUID drawId, TicketStatus status, Instant createdAt) {
        return new Ticket(
                UUID.randomUUID(),
                userId,
                drawId,
                status,
                new Combination(List.of("1", "2")),
                new Money(new BigDecimal("10.00"), Currency.getInstance("USD")),
                null,
                null,
                false,
                createdAt,
                null,
                null,
                null,
                null,
                null,
                0);
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
                return false;
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

    private static AuditService audit(AuditLogs auditLogs) {
        return new AuditService(auditLogs, new Rbac(), () -> NOW);
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
        public List<Draw> findReport(
                UUID drawId,
                UUID managerId,
                DrawStatus status,
                Instant createdFrom,
                Instant createdTo,
                int limit,
                int offset) {
            return filtered(drawId, managerId, status, createdFrom, createdTo).skip(offset).limit(limit).toList();
        }

        @Override
        public long countReport(UUID drawId, UUID managerId, DrawStatus status, Instant createdFrom, Instant createdTo) {
            return filtered(drawId, managerId, status, createdFrom, createdTo).count();
        }

        private Stream<Draw> filtered(UUID drawId, UUID managerId, DrawStatus status, Instant createdFrom, Instant createdTo) {
            return draws.stream()
                    .filter(draw -> drawId == null || draw.id().equals(drawId))
                    .filter(draw -> managerId == null || draw.managerId().orElse(null).equals(managerId))
                    .filter(draw -> status == null || draw.status() == status)
                    .filter(draw -> createdFrom == null || !draw.createdAt().isBefore(createdFrom))
                    .filter(draw -> createdTo == null || !draw.createdAt().isAfter(createdTo))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()));
        }
    }

    private static final class Tickets implements TicketRepository {
        private final List<Ticket> tickets;

        private Tickets(List<Ticket> tickets) {
            this.tickets = tickets;
        }

        @Override
        public Ticket save(Ticket ticket) {
            return ticket;
        }

        @Override
        public Ticket update(Ticket ticket) {
            return ticket;
        }

        @Override
        public Optional<Ticket> findById(UUID id) {
            return tickets.stream().filter(ticket -> ticket.id().equals(id)).findFirst();
        }

        @Override
        public List<Ticket> findAll(int limit, int offset) {
            return tickets.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<Ticket> findByUserId(UUID userId, int limit, int offset) {
            return tickets.stream().filter(ticket -> ticket.userId().equals(userId)).skip(offset).limit(limit).toList();
        }

        @Override
        public List<Ticket> findPaidByDrawId(UUID drawId) {
            return tickets.stream().filter(ticket -> ticket.drawId().equals(drawId) && ticket.status() == TicketStatus.PAID).toList();
        }

        @Override
        public List<Ticket> findReport(
                UUID userId,
                UUID drawId,
                TicketStatus status,
                Instant createdFrom,
                Instant createdTo,
                int limit,
                int offset) {
            return filtered(userId, drawId, status, createdFrom, createdTo).skip(offset).limit(limit).toList();
        }

        @Override
        public long countReport(UUID userId, UUID drawId, TicketStatus status, Instant createdFrom, Instant createdTo) {
            return filtered(userId, drawId, status, createdFrom, createdTo).count();
        }

        private Stream<Ticket> filtered(UUID userId, UUID drawId, TicketStatus status, Instant createdFrom, Instant createdTo) {
            return tickets.stream()
                    .filter(ticket -> userId == null || ticket.userId().equals(userId))
                    .filter(ticket -> drawId == null || ticket.drawId().equals(drawId))
                    .filter(ticket -> status == null || ticket.status() == status)
                    .filter(ticket -> createdFrom == null || !ticket.createdAt().isBefore(createdFrom))
                    .filter(ticket -> createdTo == null || !ticket.createdAt().isAfter(createdTo))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()));
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

    private static final class AuditLogs implements AuditLogRepository {
        private final List<AuditLog> items = new java.util.ArrayList<>();

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
