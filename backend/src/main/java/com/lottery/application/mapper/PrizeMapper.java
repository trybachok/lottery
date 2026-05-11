package com.lottery.application.mapper;

import com.lottery.application.dto.PrizeDto;
import com.lottery.domain.model.Prize;

public final class PrizeMapper {
    public PrizeDto toDto(Prize prize) {
        return new PrizeDto(
                prize.id(),
                prize.type(),
                prize.name(),
                prize.amount(),
                prize.currency() == null ? null : prize.currency().getCurrencyCode(),
                prize.productId(),
                prize.quantity(),
                prize.unit());
    }
}
