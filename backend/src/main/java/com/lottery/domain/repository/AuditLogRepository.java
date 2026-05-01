package com.lottery.domain.repository;

import com.lottery.domain.model.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    void append(AuditLog auditLog);

    List<AuditLog> find(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Instant createdFrom,
            Instant createdTo,
            int limit,
            int offset);
}
