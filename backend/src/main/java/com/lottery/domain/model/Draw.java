package com.lottery.domain.model;

import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.DrawStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Draw {
    private final UUID id;
    private final String title;
    private final String description;
    private final DrawStatus status;
    private final UUID managerId;
    private final UUID combinationSchemaId;
    private final UUID uiThemeId;
    private final UUID uiTemplateId;
    private final Instant salesStartAt;
    private final Instant salesEndAt;
    private final Instant drawAt;
    private final Integer maxTickets;
    private final boolean test;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final long version;

    public Draw(
            UUID id,
            String title,
            String description,
            DrawStatus status,
            UUID managerId,
            UUID combinationSchemaId,
            UUID uiThemeId,
            UUID uiTemplateId,
            Instant salesStartAt,
            Instant salesEndAt,
            Instant drawAt,
            Integer maxTickets,
            boolean test,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = requireNonBlank(title, "title");
        this.description = description == null ? "" : description;
        this.status = Objects.requireNonNull(status, "status");
        this.managerId = managerId;
        this.combinationSchemaId = Objects.requireNonNull(combinationSchemaId, "combinationSchemaId");
        this.uiThemeId = uiThemeId;
        this.uiTemplateId = uiTemplateId;
        this.salesStartAt = Objects.requireNonNull(salesStartAt, "salesStartAt");
        this.salesEndAt = Objects.requireNonNull(salesEndAt, "salesEndAt");
        this.drawAt = Objects.requireNonNull(drawAt, "drawAt");
        if (!salesStartAt.isBefore(salesEndAt)) {
            throw new IllegalArgumentException("salesStartAt must be before salesEndAt");
        }
        if (drawAt.isBefore(salesEndAt)) {
            throw new IllegalArgumentException("drawAt must not be before salesEndAt");
        }
        this.maxTickets = maxTickets;
        if (maxTickets != null && maxTickets <= 0) {
            throw new IllegalArgumentException("maxTickets must be positive");
        }
        this.test = test;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Draw create(
            String title,
            String description,
            UUID managerId,
            UUID combinationSchemaId,
            Instant salesStartAt,
            Instant salesEndAt,
            Instant drawAt,
            Integer maxTickets,
            boolean test,
            Instant now) {
        DrawStatus initialStatus = test ? DrawStatus.TEST : DrawStatus.DRAFT;
        return new Draw(
                DomainIds.newId(),
                title,
                description,
                initialStatus,
                managerId,
                combinationSchemaId,
                null,
                null,
                salesStartAt,
                salesEndAt,
                drawAt,
                maxTickets,
                test,
                now,
                now,
                null,
                0);
    }

    public Draw withStatus(DrawStatus newStatus, Instant now) {
        return new Draw(
                id,
                title,
                description,
                newStatus,
                managerId,
                combinationSchemaId,
                uiThemeId,
                uiTemplateId,
                salesStartAt,
                salesEndAt,
                drawAt,
                maxTickets,
                test,
                createdAt,
                now,
                deletedAt,
                version);
    }

    public Draw withManager(UUID newManagerId, Instant now) {
        return new Draw(
                id,
                title,
                description,
                status,
                newManagerId,
                combinationSchemaId,
                uiThemeId,
                uiTemplateId,
                salesStartAt,
                salesEndAt,
                drawAt,
                maxTickets,
                test,
                createdAt,
                now,
                deletedAt,
                version);
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public DrawStatus status() {
        return status;
    }

    public Optional<UUID> managerId() {
        return Optional.ofNullable(managerId);
    }

    public UUID combinationSchemaId() {
        return combinationSchemaId;
    }

    public Optional<UUID> uiThemeId() {
        return Optional.ofNullable(uiThemeId);
    }

    public Optional<UUID> uiTemplateId() {
        return Optional.ofNullable(uiTemplateId);
    }

    public Instant salesStartAt() {
        return salesStartAt;
    }

    public Instant salesEndAt() {
        return salesEndAt;
    }

    public Instant drawAt() {
        return drawAt;
    }

    public Optional<Integer> maxTickets() {
        return Optional.ofNullable(maxTickets);
    }

    public boolean test() {
        return test;
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
