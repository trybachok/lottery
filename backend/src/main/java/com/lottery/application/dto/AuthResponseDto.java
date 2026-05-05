package com.lottery.application.dto;

import java.time.Instant;
import java.util.Set;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserDto user,
        Set<String> roleCodes,
        Set<String> permissions) {
    public AuthResponseDto {
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
