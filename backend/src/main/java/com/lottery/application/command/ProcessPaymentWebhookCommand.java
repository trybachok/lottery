package com.lottery.application.command;

import java.util.Objects;

public record ProcessPaymentWebhookCommand(String providerCode, String payloadJson, String signature) {
    public ProcessPaymentWebhookCommand {
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(payloadJson, "payloadJson");
        if (providerCode.isBlank() || payloadJson.isBlank()) {
            throw new IllegalArgumentException("providerCode and payloadJson must not be blank");
        }
    }
}
