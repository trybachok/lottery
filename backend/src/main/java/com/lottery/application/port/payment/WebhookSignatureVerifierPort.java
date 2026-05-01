package com.lottery.application.port.payment;

public interface WebhookSignatureVerifierPort {
    boolean isValid(String providerCode, String payload, String signature);
}
