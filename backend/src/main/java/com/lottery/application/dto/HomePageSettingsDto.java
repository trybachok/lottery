package com.lottery.application.dto;

import java.time.Instant;
import java.util.UUID;

public record HomePageSettingsDto(
        UUID activeTemplateId,
        UUID defaultThemeId,
        UUID updatedBy,
        Instant updatedAt) {
}
