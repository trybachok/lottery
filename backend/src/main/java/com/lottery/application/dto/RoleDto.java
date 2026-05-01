package com.lottery.application.dto;

import java.util.UUID;

public record RoleDto(UUID id, String code, String name, String description, boolean system) {
}
