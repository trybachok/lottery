package com.lottery.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        UUID invoiceId,
        String providerCode,
        String status,
        BigDecimal amount,
        String currency,
        String externalPaymentId,
        Instant createdAt,
        Instant updatedAt) {
}
