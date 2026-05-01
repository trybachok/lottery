package com.lottery.application.dto;

import java.time.Instant;

public record AuthResponseDto(String accessToken, String tokenType, Instant expiresAt, UserDto user) {
}
