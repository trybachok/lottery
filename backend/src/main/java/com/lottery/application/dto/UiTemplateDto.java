package com.lottery.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UiTemplateDto(
        UUID id,
        String name,
        Map<String, Object> layout,
        Instant createdAt) {
    public UiTemplateDto {
        layout = Map.copyOf(layout);
    }
}
