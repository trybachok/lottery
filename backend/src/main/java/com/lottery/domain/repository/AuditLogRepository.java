package com.lottery.domain.repository;

import com.lottery.domain.model.AuditLog;

public interface AuditLogRepository {
    void append(AuditLog auditLog);
}
