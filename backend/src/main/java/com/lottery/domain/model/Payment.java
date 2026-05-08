package com.lottery.domain.model;

import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PaymentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID invoiceId,
        String providerCode,
        PaymentStatus status,
        Money amount,
        String externalPaymentId,
        Instant createdAt,
        Instant updatedAt) {
    public Payment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Optional<String> maybeExternalPaymentId() {
        return Optional.ofNullable(externalPaymentId);
    }

    public Payment withStatus(PaymentStatus newStatus, Instant now) {
        return new Payment(id, invoiceId, providerCode, newStatus, amount, externalPaymentId, createdAt, now);
    }

    public Payment withExternalPaymentId(String newExternalPaymentId, Instant now) {
        return new Payment(id, invoiceId, providerCode, status, amount, newExternalPaymentId, createdAt, now);
    }
}
