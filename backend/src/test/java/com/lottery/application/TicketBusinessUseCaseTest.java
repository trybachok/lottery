package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lottery.application.command.BulkCreateTicketsCommand;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationValidatorPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.ticket.BulkCreateTicketsUseCase;
import com.lottery.application.usecase.ticket.CancelTicketUseCase;
import com.lottery.application.usecase.ticket.CheckTicketResultUseCase;
import com.lottery.application.usecase.ticket.CreateTicketUseCase;
import com.lottery.application.usecase.ticket.TicketCreationService;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.model.User;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.domain.valueobject.TicketStatus;
import com.lottery.domain.valueobject.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TicketBusinessUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");
    private static final Currency RUB = Currency.getInstance("RUB");

    @Test
    void createRejectsClientBuyingForAnotherUser() {
        Fixture fixture = new Fixture(10, validatorOk());
        UUID otherUserId = UUID.randomUUID();
        fixture.users.users.put(otherUserId, user(otherUserId));

        assertThrows(
                ForbiddenException.class,
                () -> fixture.createUseCase.execute(command(otherUserId, fixture.draw.id(), List.of("1", "2")), clientContext(fixture.user.id())));
    }

    @Test
    void createValidatesCombinationAgainstSchema() {
        Fixture fixture = new Fixture(10, (combination, schema) -> {
            throw new IllegalArgumentException("bad combination");
        });

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.createUseCase.execute(command(fixture.user.id(), fixture.draw.id(), List.of("9", "9")), clientContext(fixture.user.id())));
    }

    @Test
    void bulkCreateRespectsDrawMaxTicketsWithinSameBatch() {
        Fixture fixture = new Fixture(1, validatorOk());
        BulkCreateTicketsUseCase useCase = new BulkCreateTicketsUseCase(tx(), fixture.creationService, new TicketMapper());

        assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        new BulkCreateTicketsCommand(List.of(
                                command(fixture.user.id(), fixture.draw.id(), List.of("1", "2")),
                                command(fixture.user.id(), fixture.draw.id(), List.of("3", "4")))),
                        clientContext(fixture.user.id())));
    }

    @Test
    void bulkCreateAllowsFillingDrawExactlyToMaxTickets() {
        Fixture fixture = new Fixture(2, validatorOk());
        BulkCreateTicketsUseCase useCase = new BulkCreateTicketsUseCase(tx(), fixture.creationService, new TicketMapper());

        var result = useCase.execute(
                new BulkCreateTicketsCommand(List.of(
                        command(fixture.user.id(), fixture.draw.id(), List.of("1", "2")),
                        command(fixture.user.id(), fixture.draw.id(), List.of("3", "4")))),
                clientContext(fixture.user.id()));

        assertEquals(2, result.items().size());
        assertEquals(2, fixture.tickets.countActiveByDrawId(fixture.draw.id()));
    }

    @Test
    void cancelOwnCreatedTicket() {
        Fixture fixture = new Fixture(10, validatorOk());
        Ticket ticket = fixture.tickets.save(ticket(fixture.user.id(), fixture.draw.id(), TicketStatus.CREATED));
        CancelTicketUseCase useCase = new CancelTicketUseCase(fixture.tickets, auth(Set.of(RoleCodes.CLIENT)), tx(), clock(), new TicketMapper());

        var result = useCase.execute(ticket.id(), clientContext(fixture.user.id()));

        assertEquals(TicketStatus.CANCELLED.name(), result.status());
        assertEquals(TicketStatus.CANCELLED, fixture.tickets.byId.get(ticket.id()).status());
    }

    @Test
    void checkResultRequiresTicketWinOrLose() {
        Fixture fixture = new Fixture(10, validatorOk());
        Ticket ticket = fixture.tickets.save(ticket(fixture.user.id(), fixture.draw.id(), TicketStatus.PAID));
        CheckTicketResultUseCase useCase = new CheckTicketResultUseCase(
                fixture.tickets,
                new Results(Set.of(fixture.draw.id())),
                auth(Set.of(RoleCodes.CLIENT)),
                tx(),
                new TicketMapper());

        assertThrows(ConflictException.class, () -> useCase.execute(ticket.id(), clientContext(fixture.user.id())));
    }

    private static CreateTicketCommand command(UUID userId, UUID drawId, List<String> combination) {
        return new CreateTicketCommand(userId, drawId, combination, BigDecimal.TEN, RUB, false);
    }

    private static UseCaseContext clientContext(UUID userId) {
        return new UseCaseContext(
                userId,
                Set.of(PermissionCodes.TICKET_CREATE, PermissionCodes.TICKET_READ, PermissionCodes.TICKET_CANCEL),
                "req",
                Set.of(RoleCodes.CLIENT),
                null,
                null);
    }

    private static CombinationValidatorPort validatorOk() {
        return (combination, schema) -> {
        };
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

    private static User user(UUID id) {
        return new User(id, "client-" + id + "@example.com", "client-" + id, "hash", UserStatus.ACTIVE, NOW, NOW, null, 0);
    }

    private static Draw draw(UUID schemaId, Integer maxTickets) {
        return new Draw(
                UUID.randomUUID(),
                "Draw",
                "",
                DrawStatus.ACTIVE,
                null,
                schemaId,
                null,
                null,
                NOW.minusSeconds(60),
                NOW.plusSeconds(60),
                NOW.plusSeconds(120),
                maxTickets,
                false,
                NOW.minusSeconds(120),
                NOW.minusSeconds(120),
                null,
                0);
    }

    private static Ticket ticket(UUID userId, UUID drawId, TicketStatus status) {
        return new Ticket(
                UUID.randomUUID(),
                userId,
                drawId,
                status,
                new Combination(List.of("1", "2")),
                new Money(BigDecimal.TEN, RUB),
                null,
                null,
                false,
                NOW,
                status == TicketStatus.PAID ? NOW : null,
                null,
                null,
                null,
                0);
    }

    private static final class Fixture {
        private final UUID schemaId = UUID.randomUUID();
        private final User user = user(UUID.randomUUID());
        private final Draw draw;
        private final Users users = new Users(user);
        private final Draws draws;
        private final Tickets tickets = new Tickets();
        private final Schemas schemas;
        private final TicketCreationService creationService;
        private final CreateTicketUseCase createUseCase;

        private Fixture(Integer maxTickets, CombinationValidatorPort validator) {
            draw = draw(schemaId, maxTickets);
            draws = new Draws(draw);
            schemas = new Schemas(schemaId);
            creationService = new TicketCreationService(
                    users,
                    draws,
                    tickets,
                    schemas,
                    auth(Set.of(RoleCodes.CLIENT)),
                    validator,
                    clock(),
                    new TicketPurchasePolicy());
            createUseCase = new CreateTicketUseCase(tx(), creationService, new TicketMapper());
        }
    }

    private static final class Users implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();

        private Users(User user) {
            users.put(user.id(), user);
        }

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

    private static final class Draws implements DrawRepository {
        private final Draw draw;

        private Draws(Draw draw) {
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

    private static final class Schemas implements CombinationSchemaRepository {
        private final UUID schemaId;

        private Schemas(UUID schemaId) {
            this.schemaId = schemaId;
        }

        @Override
        public CombinationSchema save(CombinationSchema schema) {
            return schema;
        }

        @Override
        public Optional<CombinationSchema> findById(UUID id) {
            return schemaId.equals(id)
                    ? Optional.of(new CombinationSchema(id, "schema", new CombinationSchemaDefinition("{\"positions\":[]}"), NOW))
                    : Optional.empty();
        }
    }

    private static final class Tickets implements TicketRepository {
        private final Map<UUID, Ticket> byId = new HashMap<>();

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
            return byId.values().stream().filter(ticket -> ticket.userId().equals(userId)).skip(offset).limit(limit).toList();
        }

        @Override
        public List<Ticket> findPaidByDrawId(UUID drawId) {
            return byId.values().stream()
                    .filter(ticket -> ticket.drawId().equals(drawId) && ticket.status() == TicketStatus.PAID)
                    .toList();
        }

        @Override
        public long countActiveByDrawId(UUID drawId) {
            return byId.values().stream()
                    .filter(ticket -> ticket.drawId().equals(drawId) && ticket.deletedAt().isEmpty())
                    .count();
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
}
