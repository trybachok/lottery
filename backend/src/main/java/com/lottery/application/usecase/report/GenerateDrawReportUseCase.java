package com.lottery.application.usecase.report;

import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.dto.ReportPageDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.DrawReportQuery;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenerateDrawReportUseCase {
    private static final Logger log = LoggerFactory.getLogger(GenerateDrawReportUseCase.class);

    private final DrawRepository drawRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DrawMapper mapper;
    private final AuditService auditService;

    public GenerateDrawReportUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public ReportPageDto<DrawDto> execute(DrawReportQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.REPORT_DRAW_EXPORT);
            List<DrawDto> report = drawRepository
                    .findReport(query.drawId(), query.userId(), query.status(), query.dateFrom(), query.dateTo(), query.limit(), query.offset())
                    .stream()
                    .map(mapper::toDto)
                    .toList();
            long total = drawRepository.countReport(query.drawId(), query.userId(), query.status(), query.dateFrom(), query.dateTo());
            auditService.record(context, "REPORT_DRAW_EXPORT", "REPORT", null);
            log.info(
                    "requestId={} actorUserId={} report=draws rows={} total={} limit={} offset={}",
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
