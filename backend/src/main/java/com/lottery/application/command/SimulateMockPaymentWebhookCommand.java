package com.lottery.application.command;

import java.util.Objects;
import java.util.UUID;

public record SimulateMockPaymentWebhookCommand(UUID invoiceId, String eventType) {
    public SimulateMockPaymentWebhookCommand {
        Objects.requireNonNull(invoiceId, "invoiceId");
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
    }
}
