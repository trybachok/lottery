package com.lottery.application.command;

import java.util.Objects;
import java.util.UUID;

public record CreateInvoiceForTicketCommand(UUID ticketId, String providerCode, String idempotencyKey) {
    public CreateInvoiceForTicketCommand {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (providerCode.isBlank() || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("providerCode and idempotencyKey must not be blank");
        }
    }
}
