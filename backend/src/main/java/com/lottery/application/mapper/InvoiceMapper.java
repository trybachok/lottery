package com.lottery.application.mapper;

import com.lottery.application.dto.InvoiceDto;
import com.lottery.domain.model.Invoice;

public final class InvoiceMapper {
    public InvoiceDto toDto(Invoice invoice, String paymentUrl) {
        return new InvoiceDto(
                invoice.id(),
                invoice.ticketId(),
                invoice.userId(),
                invoice.providerCode(),
                invoice.status().name(),
                invoice.amount().amount(),
                invoice.amount().currency().getCurrencyCode(),
                invoice.maybeExternalInvoiceId().orElse(null),
                paymentUrl == null ? invoice.maybePaymentUrl().orElse(null) : paymentUrl,
                invoice.createdAt(),
                invoice.maybeExpiresAt().orElse(null),
                invoice.maybePaidAt().orElse(null));
    }
}
