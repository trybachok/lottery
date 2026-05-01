package com.lottery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentWebhookEvent(
        UUID id,
        String providerCode,
        String eventType,
        String externalEventId,
        String payloadJson,
        boolean signatureValid,
        boolean processed,
        Instant receivedAt) {
    public PaymentWebhookEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(externalEventId, "externalEventId");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    public PaymentWebhookEvent markProcessed() {
        return new PaymentWebhookEvent(id, providerCode, eventType, externalEventId, payloadJson, signatureValid, true, receivedAt);
    }
}
