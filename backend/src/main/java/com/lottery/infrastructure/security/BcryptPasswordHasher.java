package com.lottery.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.lottery.application.port.auth.PasswordHasher;

public final class BcryptPasswordHasher implements PasswordHasher {
    private final int cost;

    public BcryptPasswordHasher(int cost) {
        if (cost < 10) {
            throw new IllegalArgumentException("BCrypt cost must be at least 10");
        }
        this.cost = cost;
    }

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }
        return BCrypt.withDefaults().hashToString(cost, rawPassword.toCharArray());
    }

    @Override
    public boolean verify(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), passwordHash).verified;
    }
}
