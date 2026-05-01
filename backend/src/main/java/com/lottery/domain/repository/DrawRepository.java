package com.lottery.domain.repository;

import com.lottery.domain.model.Draw;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepository {
    Draw save(Draw draw);

    Draw update(Draw draw);

    Optional<Draw> findById(UUID id);

    List<Draw> findAll(int limit, int offset);
}
