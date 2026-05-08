package com.lottery.application.command;

import java.util.UUID;

public record CancelInvoiceCommand(UUID invoiceId, String idempotencyKey) {
    public CancelInvoiceCommand {
        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }
}
