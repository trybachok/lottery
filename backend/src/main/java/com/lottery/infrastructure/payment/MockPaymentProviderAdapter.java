package com.lottery.infrastructure.payment;

import com.lottery.application.ApplicationException;
import com.lottery.application.port.payment.PaymentProviderPort;
import com.lottery.application.port.payment.WebhookSignatureVerifierPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class MockPaymentProviderAdapter implements PaymentProviderPort, WebhookSignatureVerifierPort {
    public static final String PROVIDER_CODE = "mock";

    private final String webhookSecret;

    public MockPaymentProviderAdapter(String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException("webhookSecret must not be blank");
        }
        this.webhookSecret = webhookSecret;
    }

    @Override
    public InvoiceCreationResult createInvoice(InvoiceCreationRequest request) {
        if (!PROVIDER_CODE.equals(request.providerCode())) {
            throw new ApplicationException("PAYMENT_PROVIDER_NOT_CONFIGURED", "Payment provider adapter is not configured");
        }
        String externalInvoiceId = "mock_inv_" + request.ticketId();
        String externalPaymentId = "mock_pay_" + request.ticketId();
        return new InvoiceCreationResult(
                PROVIDER_CODE,
                externalInvoiceId,
                externalPaymentId,
                "https://mock-payments.local/invoices/" + externalInvoiceId);
    }

    @Override
    public PaymentStatusResult checkStatus(PaymentStatusRequest request) {
        return new PaymentStatusResult("INITIATED", false);
    }

    @Override
    public void cancelPayment(PaymentCancellationRequest request) {
    }

    @Override
    public RefundResult refundPayment(RefundRequest request) {
        return new RefundResult("mock_refund_" + request.externalPaymentId(), "REFUNDED");
    }

    @Override
    public boolean isValid(String providerCode, String payload, String signature) {
        if (!PROVIDER_CODE.equals(providerCode) || signature == null || signature.isBlank()) {
            return false;
        }
        String expected = hmacSha256(payload);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate mock payment webhook signature", exception);
        }
    }
}
