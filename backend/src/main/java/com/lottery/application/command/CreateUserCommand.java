package com.lottery.application.command;

import java.util.Objects;

public record CreateUserCommand(String email, String login, String rawPassword) {
    public CreateUserCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(login, "login");
        if (email.isBlank() || login.isBlank()) {
            throw new IllegalArgumentException("email and login must not be blank");
        }
    }
}
