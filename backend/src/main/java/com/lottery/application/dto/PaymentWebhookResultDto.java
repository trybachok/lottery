package com.lottery.application.dto;

import java.util.UUID;

public record PaymentWebhookResultDto(UUID eventId, boolean processed, boolean duplicate, String status) {
}
