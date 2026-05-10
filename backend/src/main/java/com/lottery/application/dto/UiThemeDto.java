package com.lottery.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UiThemeDto(
        UUID id,
        String name,
        Map<String, Object> tokens,
        boolean defaultTheme,
        Instant createdAt) {
    public UiThemeDto {
        tokens = Map.copyOf(tokens);
    }
}
