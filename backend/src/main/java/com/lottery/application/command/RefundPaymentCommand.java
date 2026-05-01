package com.lottery.application.command;

import java.util.Objects;
import java.util.UUID;

public record RefundPaymentCommand(UUID paymentId, String idempotencyKey) {
    public RefundPaymentCommand {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
