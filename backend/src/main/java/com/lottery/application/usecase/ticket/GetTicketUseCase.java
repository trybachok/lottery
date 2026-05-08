package com.lottery.application.usecase.ticket;

import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.UUID;

public final class GetTicketUseCase {
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final TicketMapper mapper;

    public GetTicketUseCase(
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            TicketMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public TicketDto execute(UUID ticketId, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_READ);
        return transactionManager.inTransaction(() -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
            TicketAccess.ensureCanAccess(ticket, context, authorizationPort, PermissionCodes.TICKET_READ);
            return mapper.toDto(ticket);
        });
    }
}

