package com.lottery.application.usecase.ticket;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.UUID;

public final class DeleteTicketUseCase {
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;

    public DeleteTicketUseCase(
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock) {
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
    }

    public void execute(UUID ticketId, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CANCEL);
        transactionManager.inTransaction(() -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
            TicketAccess.ensureCanAccess(ticket, context, authorizationPort, PermissionCodes.TICKET_CANCEL);
            try {
                ticketRepository.update(ticket.withDeleted(clock.now()));
            } catch (IllegalStateException exception) {
                throw new ConflictException("TICKET_DELETE_FORBIDDEN", exception.getMessage());
            }
            return null;
        });
    }
}

