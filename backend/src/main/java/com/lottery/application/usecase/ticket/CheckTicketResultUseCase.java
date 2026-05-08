package com.lottery.application.usecase.ticket;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.util.UUID;

public final class CheckTicketResultUseCase {
    private final TicketRepository ticketRepository;
    private final DrawResultRepository drawResultRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final TicketMapper mapper;

    public CheckTicketResultUseCase(
            TicketRepository ticketRepository,
            DrawResultRepository drawResultRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            TicketMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.drawResultRepository = drawResultRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public TicketDto execute(UUID ticketId, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_READ);
        return transactionManager.inTransaction(() -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
            TicketAccess.ensureCanAccess(ticket, context, authorizationPort, PermissionCodes.TICKET_READ);
            if (!drawResultRepository.existsByDrawId(ticket.drawId())) {
                throw new ConflictException("DRAW_RESULT_NOT_READY", "Draw result is not available yet");
            }
            if (ticket.status() != TicketStatus.WIN && ticket.status() != TicketStatus.LOSE) {
                throw new ConflictException("TICKET_RESULT_NOT_READY", "Ticket has not been checked in draw yet");
            }
            return mapper.toDto(ticket);
        });
    }
}

