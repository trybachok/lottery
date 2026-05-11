package com.lottery.domain.repository;

import com.lottery.domain.model.WinningRule;
import java.util.List;
import java.util.UUID;

public interface WinningRuleRepository {
    List<WinningRule> findByDrawIdOrderByPriority(UUID drawId);

    default WinningRule save(WinningRule winningRule) {
        throw new UnsupportedOperationException("Saving winning rules is not supported");
    }

    default void deleteByDrawId(UUID drawId) {
        throw new UnsupportedOperationException("Deleting winning rules is not supported");
    }
}
