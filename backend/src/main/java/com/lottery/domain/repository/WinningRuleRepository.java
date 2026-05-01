package com.lottery.domain.repository;

import com.lottery.domain.model.WinningRule;
import java.util.List;
import java.util.UUID;

public interface WinningRuleRepository {
    List<WinningRule> findByDrawIdOrderByPriority(UUID drawId);
}
