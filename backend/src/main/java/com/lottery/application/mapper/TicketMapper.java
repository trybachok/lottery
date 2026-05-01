package com.lottery.application.mapper;

import com.lottery.application.dto.TicketDto;
import com.lottery.domain.model.Ticket;

public final class TicketMapper {
    public TicketDto toDto(Ticket ticket) {
        return new TicketDto(
                ticket.id(),
                ticket.userId(),
                ticket.drawId(),
                ticket.status().name(),
                ticket.combination().values(),
                ticket.price().amount(),
                ticket.price().currency().getCurrencyCode(),
                ticket.test(),
                ticket.createdAt(),
                ticket.version());
    }
}
