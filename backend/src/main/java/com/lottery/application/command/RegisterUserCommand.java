package com.lottery.application.command;

import java.util.Objects;

public record RegisterUserCommand(String email, String login, String rawPassword) {
    public RegisterUserCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(rawPassword, "rawPassword");
        if (email.isBlank() || rawPassword.isBlank()) {
            throw new IllegalArgumentException("email and password must not be blank");
        }
        if (login != null && login.isBlank()) {
            throw new IllegalArgumentException("login must not be blank when provided");
        }
    }
}
