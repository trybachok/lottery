package com.lottery.application.usecase.audit;

import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.AuditLogDto;
import com.lottery.application.mapper.AuditLogMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.ListAuditLogsQuery;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.List;

public final class ListAuditLogsUseCase {
    private final AuditLogRepository auditLogRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final AuditLogMapper mapper;

    public ListAuditLogsUseCase(
            AuditLogRepository auditLogRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            AuditLogMapper mapper) {
        this.auditLogRepository = auditLogRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public List<AuditLogDto> execute(ListAuditLogsQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.AUDIT_READ);
            return auditLogRepository
                    .find(
                            query.actorUserId(),
                            query.action(),
                            query.entityType(),
                            query.entityId(),
                            query.dateFrom(),
                            query.dateTo(),
                            query.limit(),
                            query.offset())
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        });
    }
}
