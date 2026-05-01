package com.lottery.application.command;

import java.util.Objects;

public record LoginByPasswordCommand(String loginOrEmail, String rawPassword) {
    public LoginByPasswordCommand {
        Objects.requireNonNull(loginOrEmail, "loginOrEmail");
        Objects.requireNonNull(rawPassword, "rawPassword");
        if (loginOrEmail.isBlank() || rawPassword.isBlank()) {
            throw new IllegalArgumentException("loginOrEmail and password must not be blank");
        }
    }
}
