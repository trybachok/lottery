package com.lottery.domain.repository;

import com.lottery.domain.model.UiTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UiTemplateRepository {
    List<UiTemplate> findAll(int limit, int offset);

    Optional<UiTemplate> findById(UUID id);

    UiTemplate save(UiTemplate template);

    UiTemplate update(UiTemplate template);
}
