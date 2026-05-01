package com.lottery.domain.model;

import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Invoice(
        UUID id,
        UUID ticketId,
        UUID userId,
        String providerCode,
        InvoiceStatus status,
        Money amount,
        String externalInvoiceId,
        String idempotencyKey,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt) {
    public Invoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public Optional<String> maybeExternalInvoiceId() {
        return Optional.ofNullable(externalInvoiceId);
    }

    public Optional<Instant> maybeExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public Optional<Instant> maybePaidAt() {
        return Optional.ofNullable(paidAt);
    }
}
