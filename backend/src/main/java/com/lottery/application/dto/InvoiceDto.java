package com.lottery.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceDto(
        UUID id,
        UUID ticketId,
        UUID userId,
        String providerCode,
        String status,
        BigDecimal amount,
        String currency,
        String externalInvoiceId,
        String paymentUrl,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt) {
}
