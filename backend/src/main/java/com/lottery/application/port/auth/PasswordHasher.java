package com.lottery.application.port.auth;

public interface PasswordHasher {
    String hash(String rawPassword);
}
