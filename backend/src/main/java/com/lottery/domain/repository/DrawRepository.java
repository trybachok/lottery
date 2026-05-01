package com.lottery.domain.repository;

import com.lottery.domain.model.Draw;
import com.lottery.domain.valueobject.DrawStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepository {
    Draw save(Draw draw);

    Draw update(Draw draw);

    Optional<Draw> findById(UUID id);

    Optional<Draw> findByIdForUpdate(UUID id);

    List<Draw> findAll(int limit, int offset);

    default List<Draw> findReport(
            UUID drawId,
            UUID managerId,
            DrawStatus status,
            Instant createdFrom,
            Instant createdTo,
            int limit,
            int offset) {
        return findAll(limit, offset);
    }
}
