package com.lottery.domain.model;

import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.UserStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class User {
    private final UUID id;
    private final String email;
    private final String login;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final long version;

    public User(
            UUID id,
            String email,
            String login,
            String passwordHash,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = requireNonBlank(email, "email");
        this.login = requireNonBlank(login, "login");
        this.passwordHash = passwordHash;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static User create(String email, String login, String passwordHash, Instant now) {
        return new User(DomainIds.newId(), email, login, passwordHash, UserStatus.ACTIVE, now, now, null, 0);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String login() {
        return login;
    }

    public Optional<String> passwordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> deletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public long version() {
        return version;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
