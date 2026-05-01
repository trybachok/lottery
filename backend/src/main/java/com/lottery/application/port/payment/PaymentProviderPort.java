package com.lottery.application.port.payment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public interface PaymentProviderPort {
    InvoiceCreationResult createInvoice(InvoiceCreationRequest request);

    PaymentStatusResult checkStatus(PaymentStatusRequest request);

    void cancelPayment(PaymentCancellationRequest request);

    RefundResult refundPayment(RefundRequest request);

    record InvoiceCreationRequest(
            UUID ticketId,
            UUID userId,
            BigDecimal amount,
            Currency currency,
            String idempotencyKey) {
    }

    record InvoiceCreationResult(String providerCode, String externalInvoiceId, String paymentUrl) {
    }

    record PaymentStatusRequest(String providerCode, String externalPaymentId) {
    }

    record PaymentStatusResult(String status, boolean providerConfirmed) {
    }

    record PaymentCancellationRequest(String providerCode, String externalPaymentId, String idempotencyKey) {
    }

    record RefundRequest(String providerCode, String externalPaymentId, BigDecimal amount, Currency currency, String idempotencyKey) {
    }

    record RefundResult(String externalRefundId, String status) {
    }
}
