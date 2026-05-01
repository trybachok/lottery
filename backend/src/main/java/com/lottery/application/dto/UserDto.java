package com.lottery.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String email, String login, String status, Instant createdAt, long version) {
}
