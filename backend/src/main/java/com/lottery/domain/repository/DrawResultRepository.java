package com.lottery.domain.repository;

import com.lottery.domain.model.DrawResult;
import java.util.Optional;
import java.util.UUID;

public interface DrawResultRepository {
    DrawResult save(DrawResult drawResult);

    Optional<DrawResult> findByDrawId(UUID drawId);

    boolean existsByDrawId(UUID drawId);
}
