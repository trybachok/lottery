package com.lottery.infrastructure.payment;

import com.lottery.application.ApplicationException;
import com.lottery.application.port.payment.PaymentProviderPort;

public final class RealPaymentProviderAdapter implements PaymentProviderPort {
    @Override
    public InvoiceCreationResult createInvoice(InvoiceCreationRequest request) {
        throw notConfigured();
    }

    @Override
    public PaymentStatusResult checkStatus(PaymentStatusRequest request) {
        throw notConfigured();
    }

    @Override
    public void cancelPayment(PaymentCancellationRequest request) {
        throw notConfigured();
    }

    @Override
    public RefundResult refundPayment(RefundRequest request) {
        throw notConfigured();
    }

    private ApplicationException notConfigured() {
        return new ApplicationException("PAYMENT_PROVIDER_NOT_CONFIGURED", "Real payment provider adapter is not configured");
    }
}
