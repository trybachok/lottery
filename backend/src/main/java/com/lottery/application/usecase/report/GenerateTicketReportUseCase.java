package com.lottery.application.usecase.report;

import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.TicketReportQuery;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;

public final class GenerateTicketReportUseCase {
    private final TicketRepository ticketRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final TicketMapper mapper;
    private final AuditService auditService;

    public GenerateTicketReportUseCase(
            TicketRepository ticketRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            TicketMapper mapper,
            AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<TicketDto> execute(TicketReportQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.REPORT_TICKET_EXPORT);
            List<TicketDto> report = ticketRepository
                    .findReport(query.userId(), query.drawId(), query.status(), query.dateFrom(), query.dateTo(), query.limit(), query.offset())
                    .stream()
                    .map(mapper::toDto)
                    .toList();
            auditService.record(context, "REPORT_TICKET_EXPORT", "REPORT", null);
            return report;
        });
    }
}
