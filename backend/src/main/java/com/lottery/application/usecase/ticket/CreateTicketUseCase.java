package com.lottery.application.usecase.ticket;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PermissionCodes;

public final class CreateTicketUseCase {
    private final UserRepository userRepository;
    private final DrawRepository drawRepository;
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final TicketPurchasePolicy ticketPurchasePolicy;
    private final TicketMapper mapper;

    public CreateTicketUseCase(
            UserRepository userRepository,
            DrawRepository drawRepository,
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            TicketPurchasePolicy ticketPurchasePolicy,
            TicketMapper mapper) {
        this.userRepository = userRepository;
        this.drawRepository = drawRepository;
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.ticketPurchasePolicy = ticketPurchasePolicy;
        this.mapper = mapper;
    }

    public TicketDto execute(CreateTicketCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CREATE);
            userRepository.findById(command.userId()).orElseThrow(() -> new NotFoundException("User"));
            Draw draw = drawRepository.findById(command.drawId()).orElseThrow(() -> new NotFoundException("Draw"));
            if (!ticketPurchasePolicy.canCreateTicketFor(draw, command.test())) {
                throw new ConflictException("DRAW_NOT_AVAILABLE_FOR_TICKET", "Draw is not available for ticket creation");
            }
            Ticket ticket = Ticket.create(
                    command.userId(),
                    command.drawId(),
                    new Combination(command.combinationValues()),
                    new Money(command.priceAmount(), command.priceCurrency()),
                    command.test(),
                    clock.now());
            return mapper.toDto(ticketRepository.save(ticket));
        });
    }
}
