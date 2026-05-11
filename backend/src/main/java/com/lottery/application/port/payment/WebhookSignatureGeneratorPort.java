package com.lottery.application.port.payment;

public interface WebhookSignatureGeneratorPort {
    String sign(String providerCode, String payload);
}
