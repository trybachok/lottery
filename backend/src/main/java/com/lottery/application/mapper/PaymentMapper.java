package com.lottery.application.mapper;

import com.lottery.application.dto.PaymentDto;
import com.lottery.domain.model.Payment;

public final class PaymentMapper {
    public PaymentDto toDto(Payment payment) {
        return new PaymentDto(
                payment.id(),
                payment.invoiceId(),
                payment.providerCode(),
                payment.status().name(),
                payment.amount().amount(),
                payment.amount().currency().getCurrencyCode(),
                payment.maybeExternalPaymentId().orElse(null),
                payment.createdAt(),
                payment.updatedAt());
    }
}
