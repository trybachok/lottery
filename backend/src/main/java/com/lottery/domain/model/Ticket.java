package com.lottery.domain.model;

import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Ticket {
    private final UUID id;
    private final UUID userId;
    private final UUID drawId;
    private final TicketStatus status;
    private final Combination combination;
    private final Money price;
    private final BigDecimal matchPercent;
    private final UUID prizeId;
    private final boolean test;
    private final Instant createdAt;
    private final Instant paidAt;
    private final Instant checkedAt;
    private final Instant cancelledAt;
    private final Instant deletedAt;
    private final long version;

    public Ticket(
            UUID id,
            UUID userId,
            UUID drawId,
            TicketStatus status,
            Combination combination,
            Money price,
            BigDecimal matchPercent,
            UUID prizeId,
            boolean test,
            Instant createdAt,
            Instant paidAt,
            Instant checkedAt,
            Instant cancelledAt,
            Instant deletedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.drawId = Objects.requireNonNull(drawId, "drawId");
        this.status = Objects.requireNonNull(status, "status");
        this.combination = Objects.requireNonNull(combination, "combination");
        this.price = Objects.requireNonNull(price, "price");
        this.matchPercent = matchPercent;
        this.prizeId = prizeId;
        this.test = test;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.paidAt = paidAt;
        this.checkedAt = checkedAt;
        this.cancelledAt = cancelledAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Ticket create(UUID userId, UUID drawId, Combination combination, Money price, boolean test, Instant now) {
        return new Ticket(
                DomainIds.newId(),
                userId,
                drawId,
                TicketStatus.CREATED,
                combination,
                price,
                null,
                null,
                test,
                now,
                null,
                null,
                null,
                null,
                0);
    }

    public Ticket withDrawResult(TicketStatus resultStatus, BigDecimal matchPercent, UUID prizeId, Instant checkedAt) {
        if (resultStatus != TicketStatus.WIN && resultStatus != TicketStatus.LOSE) {
            throw new IllegalArgumentException("Ticket result status must be WIN or LOSE");
        }
        return new Ticket(
                id,
                userId,
                drawId,
                resultStatus,
                combination,
                price,
                matchPercent,
                prizeId,
                test,
                createdAt,
                paidAt,
                checkedAt,
                cancelledAt,
                deletedAt,
                version);
    }

    public Ticket withPaymentStatus(TicketStatus newStatus, Instant now) {
        if (newStatus != TicketStatus.PAYMENT_PENDING
                && newStatus != TicketStatus.PAID
                && newStatus != TicketStatus.PAYMENT_FAILED
                && newStatus != TicketStatus.REFUND_PENDING
                && newStatus != TicketStatus.REFUNDED) {
            throw new IllegalArgumentException("Unsupported payment-related ticket status: " + newStatus);
        }
        return new Ticket(
                id,
                userId,
                drawId,
                newStatus,
                combination,
                price,
                matchPercent,
                prizeId,
                test,
                createdAt,
                newStatus == TicketStatus.PAID ? now : paidAt,
                checkedAt,
                cancelledAt,
                deletedAt,
                version);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID drawId() {
        return drawId;
    }

    public TicketStatus status() {
        return status;
    }

    public Combination combination() {
        return combination;
    }

    public Money price() {
        return price;
    }

    public Optional<BigDecimal> matchPercent() {
        return Optional.ofNullable(matchPercent);
    }

    public Optional<UUID> prizeId() {
        return Optional.ofNullable(prizeId);
    }

    public boolean test() {
        return test;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> paidAt() {
        return Optional.ofNullable(paidAt);
    }

    public Optional<Instant> checkedAt() {
        return Optional.ofNullable(checkedAt);
    }

    public Optional<Instant> cancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    public Optional<Instant> deletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public long version() {
        return version;
    }
}
