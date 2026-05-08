package com.lottery.application.dto;

import java.util.List;

public record TicketListDto(List<TicketDto> items) {
    public TicketListDto {
        items = List.copyOf(items);
    }
}

