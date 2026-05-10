package com.lottery.domain.repository;

import com.lottery.domain.model.SystemSetting;
import java.util.Optional;

public interface SystemSettingsRepository {
    Optional<SystemSetting> findByKey(String key);

    SystemSetting save(SystemSetting setting);
}
