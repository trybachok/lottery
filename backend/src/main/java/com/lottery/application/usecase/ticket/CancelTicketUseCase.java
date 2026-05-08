package com.lottery.application.usecase.ticket;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.UUID;

public final class CancelTicketUseCase {
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final TicketMapper mapper;

    public CancelTicketUseCase(
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            TicketMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public TicketDto execute(UUID ticketId, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CANCEL);
        return transactionManager.inTransaction(() -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
            TicketAccess.ensureCanAccess(ticket, context, authorizationPort, PermissionCodes.TICKET_CANCEL);
            try {
                return mapper.toDto(ticketRepository.update(ticket.withCancelled(clock.now())));
            } catch (IllegalStateException exception) {
                throw new ConflictException("TICKET_CANCEL_FORBIDDEN", exception.getMessage());
            }
        });
    }
}

