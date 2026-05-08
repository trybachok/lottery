package com.lottery.application.command;

import java.util.List;
import java.util.Objects;

public record BulkCreateTicketsCommand(List<CreateTicketCommand> tickets) {
    public BulkCreateTicketsCommand {
        Objects.requireNonNull(tickets, "tickets");
        tickets = List.copyOf(tickets);
    }
}

