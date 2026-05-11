package com.lottery.domain.repository;

import com.lottery.domain.model.Prize;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrizeRepository {
    List<Prize> findAll(int limit, int offset);

    Optional<Prize> findById(UUID id);

    Prize save(Prize prize);

    Prize update(Prize prize);
}
