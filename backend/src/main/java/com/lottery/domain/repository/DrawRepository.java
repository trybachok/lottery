package com.lottery.domain.repository;

import com.lottery.domain.model.Draw;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepository {
    Draw save(Draw draw);

    Optional<Draw> findById(UUID id);
}
