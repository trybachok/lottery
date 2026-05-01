package com.lottery.application.audit;

import com.lottery.application.UseCaseContext;
import com.lottery.domain.model.AuditLog;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final RbacRepository rbacRepository;
    private final DomainClock clock;

    public AuditService(AuditLogRepository auditLogRepository, RbacRepository rbacRepository, DomainClock clock) {
        this.auditLogRepository = auditLogRepository;
        this.rbacRepository = rbacRepository;
        this.clock = clock;
    }

    public void record(UseCaseContext context, String action, String entityType, UUID entityId) {
        Set<String> roleCodes = context.actorRoleCodes();
        if (roleCodes.isEmpty() && context.actorUserId() != null) {
            roleCodes = rbacRepository.findRoleCodesByUserId(context.actorUserId());
        }
        auditLogRepository.append(new AuditLog(
                DomainIds.newId(),
                context.actorUserId(),
                List.copyOf(roleCodes),
                action,
                entityType,
                entityId,
                context.requestId(),
                context.ipAddress(),
                context.userAgent(),
                null,
                null,
                clock.now()));
    }
}
