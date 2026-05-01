package com.lottery.application.mapper;

import com.lottery.application.dto.DrawDto;
import com.lottery.domain.model.Draw;

public final class DrawMapper {
    public DrawDto toDto(Draw draw) {
        return new DrawDto(
                draw.id(),
                draw.title(),
                draw.description(),
                draw.status().name(),
                draw.managerId().orElse(null),
                draw.combinationSchemaId(),
                draw.salesStartAt(),
                draw.salesEndAt(),
                draw.drawAt(),
                draw.maxTickets().orElse(null),
                draw.test(),
                draw.createdAt(),
                draw.version());
    }
}
