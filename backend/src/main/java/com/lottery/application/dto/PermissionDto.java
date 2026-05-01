package com.lottery.application.dto;

import java.util.UUID;

public record PermissionDto(UUID id, String code, String description) {
}
