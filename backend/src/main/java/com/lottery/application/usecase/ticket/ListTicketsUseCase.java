package com.lottery.application.usecase.ticket;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.ListTicketsQuery;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.List;
import java.util.UUID;

public final class ListTicketsUseCase {
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final TicketMapper mapper;

    public ListTicketsUseCase(
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            TicketMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public List<TicketDto> execute(ListTicketsQuery query, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.TICKET_READ);
        return transactionManager.inTransaction(() -> {
            UUID actorUserId = context.actorUserId();
            boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                    || authorizationPort.hasRole(context, RoleCodes.MANAGER);
            UUID effectiveUserId = query.userId();
            if (!privileged) {
                if (actorUserId == null) {
                    throw new ForbiddenException(PermissionCodes.TICKET_READ);
                }
                if (effectiveUserId != null && !actorUserId.equals(effectiveUserId)) {
                    throw new ForbiddenException(PermissionCodes.TICKET_READ);
                }
                effectiveUserId = actorUserId;
            }
            return (effectiveUserId == null
                            ? ticketRepository.findAll(query.limit(), query.offset())
                            : ticketRepository.findByUserId(effectiveUserId, query.limit(), query.offset()))
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        });
    }
}
