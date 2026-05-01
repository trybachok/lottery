package com.lottery.application.usecase.report;

import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.DrawReportQuery;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;

public final class GenerateDrawReportUseCase {
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

    public List<DrawDto> execute(DrawReportQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.REPORT_DRAW_EXPORT);
            List<DrawDto> report = drawRepository
                    .findReport(query.drawId(), query.userId(), query.status(), query.dateFrom(), query.dateTo(), query.limit(), query.offset())
                    .stream()
                    .map(mapper::toDto)
                    .toList();
            auditService.record(context, "REPORT_DRAW_EXPORT", "REPORT", null);
            return report;
        });
    }
}
