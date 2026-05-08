package com.lottery.domain.model;

import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.PaymentOutboxStatus;
import com.lottery.domain.valueobject.PaymentOutboxType;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PaymentOutboxMessage(
        UUID id,
        PaymentOutboxType type,
        PaymentOutboxStatus status,
        UUID invoiceId,
        UUID paymentId,
        String providerCode,
        String payloadJson,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant processedAt) {
    public PaymentOutboxMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must be non-negative");
        }
    }

    public static PaymentOutboxMessage pending(
            PaymentOutboxType type,
            UUID invoiceId,
            UUID paymentId,
            String providerCode,
            String payloadJson,
            Instant now) {
        return new PaymentOutboxMessage(
                DomainIds.newId(),
                type,
                PaymentOutboxStatus.PENDING,
                invoiceId,
                paymentId,
                providerCode,
                payloadJson,
                0,
                now,
                null,
                now,
                now,
                null);
    }

    public PaymentOutboxMessage markProcessing(Instant now) {
        return new PaymentOutboxMessage(
                id, type, PaymentOutboxStatus.PROCESSING, invoiceId, paymentId, providerCode, payloadJson,
                attempts, nextAttemptAt, lastError, createdAt, now, processedAt);
    }

    public PaymentOutboxMessage markProcessed(Instant now) {
        return new PaymentOutboxMessage(
                id, type, PaymentOutboxStatus.PROCESSED, invoiceId, paymentId, providerCode, payloadJson,
                attempts + 1, nextAttemptAt, null, createdAt, now, now);
    }

    public PaymentOutboxMessage markFailed(String error, Instant nextAttemptAt, Instant now) {
        return new PaymentOutboxMessage(
                id, type, PaymentOutboxStatus.FAILED, invoiceId, paymentId, providerCode, payloadJson,
                attempts + 1, nextAttemptAt, error, createdAt, now, processedAt);
    }

    public Optional<UUID> maybePaymentId() {
        return Optional.ofNullable(paymentId);
    }

    public Optional<String> maybeLastError() {
        return Optional.ofNullable(lastError);
    }

    public Optional<Instant> maybeProcessedAt() {
        return Optional.ofNullable(processedAt);
    }
}
