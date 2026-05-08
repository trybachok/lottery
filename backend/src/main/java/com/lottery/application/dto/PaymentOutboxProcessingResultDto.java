package com.lottery.application.dto;

public record PaymentOutboxProcessingResultDto(int processed, int failed) {
}
