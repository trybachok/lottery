package com.lottery.application.mapper;

import com.lottery.application.dto.WinningRuleDto;
import com.lottery.domain.model.WinningRule;

public final class WinningRuleMapper {
    public WinningRuleDto toDto(WinningRule rule) {
        return new WinningRuleDto(
                rule.id(),
                rule.drawId(),
                rule.matchPercentFrom(),
                rule.matchPercentTo(),
                rule.prizeId(),
                rule.priority());
    }
}
