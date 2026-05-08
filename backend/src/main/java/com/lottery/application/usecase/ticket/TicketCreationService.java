package com.lottery.application.usecase.ticket;

import com.lottery.application.ConflictException;
import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationValidatorPort;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;

public final class TicketCreationService {
    private final UserRepository userRepository;
    private final DrawRepository drawRepository;
    private final TicketRepository ticketRepository;
    private final CombinationSchemaRepository combinationSchemaRepository;
    private final AuthorizationPort authorizationPort;
    private final CombinationValidatorPort combinationValidator;
    private final DomainClock clock;
    private final TicketPurchasePolicy ticketPurchasePolicy;

    public TicketCreationService(
            UserRepository userRepository,
            DrawRepository drawRepository,
            TicketRepository ticketRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            AuthorizationPort authorizationPort,
            CombinationValidatorPort combinationValidator,
            DomainClock clock,
            TicketPurchasePolicy ticketPurchasePolicy) {
        this.userRepository = userRepository;
        this.drawRepository = drawRepository;
        this.ticketRepository = ticketRepository;
        this.combinationSchemaRepository = combinationSchemaRepository;
        this.authorizationPort = authorizationPort;
        this.combinationValidator = combinationValidator;
        this.clock = clock;
        this.ticketPurchasePolicy = ticketPurchasePolicy;
    }

    public Ticket create(CreateTicketCommand command, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CREATE);
        ensureOwnership(command, context);
        userRepository.findById(command.userId()).orElseThrow(() -> new NotFoundException("User"));
        Draw draw = drawRepository.findById(command.drawId()).orElseThrow(() -> new NotFoundException("Draw"));
        Instant now = clock.now();
        if (!ticketPurchasePolicy.canCreateTicketFor(draw, command.test(), now)) {
            throw new ConflictException("DRAW_NOT_AVAILABLE_FOR_TICKET", "Draw is not available for ticket creation");
        }
        if (draw.maxTickets().isPresent()
                && ticketRepository.countActiveByDrawId(draw.id()) >= draw.maxTickets().get()) {
            throw new ConflictException("DRAW_MAX_TICKETS_REACHED", "Draw ticket limit has been reached");
        }
        Combination combination = new Combination(command.combinationValues());
        CombinationSchema schema = combinationSchemaRepository
                .findById(draw.combinationSchemaId())
                .orElseThrow(() -> new NotFoundException("CombinationSchema"));
        combinationValidator.validate(combination, schema);
        Ticket ticket = Ticket.create(
                command.userId(),
                command.drawId(),
                combination,
                new Money(command.priceAmount(), command.priceCurrency()),
                command.test(),
                now);
        return ticketRepository.save(ticket);
    }

    private void ensureOwnership(CreateTicketCommand command, UseCaseContext context) {
        boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                || authorizationPort.hasRole(context, RoleCodes.MANAGER);
        if (!privileged && (context.actorUserId() == null || !context.actorUserId().equals(command.userId()))) {
            throw new ForbiddenException(PermissionCodes.TICKET_CREATE);
        }
    }
}
