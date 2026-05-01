package com.lottery.application.mapper;

import com.lottery.application.dto.AuditLogDto;
import com.lottery.domain.model.AuditLog;

public final class AuditLogMapper {
    public AuditLogDto toDto(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.id(),
                auditLog.actorUserId(),
                auditLog.actorRoleCodes(),
                auditLog.action(),
                auditLog.entityType(),
                auditLog.entityId(),
                auditLog.requestId(),
                auditLog.ipAddress(),
                auditLog.userAgent(),
                auditLog.beforeJson(),
                auditLog.afterJson(),
                auditLog.createdAt());
    }
}
