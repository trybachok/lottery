package com.lottery.application.usecase.report;

import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.ReportPageDto;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.TicketReportQuery;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenerateTicketReportUseCase {
    private static final Logger log = LoggerFactory.getLogger(GenerateTicketReportUseCase.class);

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

    public ReportPageDto<TicketDto> execute(TicketReportQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.REPORT_TICKET_EXPORT);
            List<TicketDto> report = ticketRepository
                    .findReport(query.userId(), query.drawId(), query.status(), query.dateFrom(), query.dateTo(), query.limit(), query.offset())
                    .stream()
                    .map(mapper::toDto)
                    .toList();
            long total = ticketRepository.countReport(query.userId(), query.drawId(), query.status(), query.dateFrom(), query.dateTo());
            auditService.record(context, "REPORT_TICKET_EXPORT", "REPORT", null);
            log.info(
                    "requestId={} actorUserId={} report=tickets rows={} total={} limit={} offset={}",
                    context.requestId(),
                    context.actorUserId(),
                    report.size(),
                    total,
                    query.limit(),
                    query.offset());
            return ReportPageDto.of(report, total, query.limit(), query.offset());
        });
    }
}
