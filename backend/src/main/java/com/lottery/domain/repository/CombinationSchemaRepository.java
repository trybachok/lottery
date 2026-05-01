package com.lottery.domain.repository;

import com.lottery.domain.model.CombinationSchema;
import java.util.Optional;
import java.util.UUID;

public interface CombinationSchemaRepository {
    Optional<CombinationSchema> findById(UUID id);
}
