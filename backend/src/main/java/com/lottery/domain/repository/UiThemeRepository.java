package com.lottery.domain.repository;

import com.lottery.domain.model.UiTheme;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UiThemeRepository {
    List<UiTheme> findAll(int limit, int offset);

    Optional<UiTheme> findById(UUID id);

    Optional<UiTheme> findDefault();

    UiTheme save(UiTheme theme);

    UiTheme update(UiTheme theme);

    void setDefault(UUID id);
}
