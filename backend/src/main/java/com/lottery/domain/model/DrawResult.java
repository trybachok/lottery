package com.lottery.domain.model;

import com.lottery.domain.valueobject.Combination;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record DrawResult(
        UUID id,
        UUID drawId,
        Combination winningCombination,
        String algorithmVersion,
        String randomProvider,
        String proofHash,
        UUID generatedBy,
        Instant generatedAt,
        String requestId,
        String correlationId) {
    public DrawResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(drawId, "drawId");
        Objects.requireNonNull(winningCombination, "winningCombination");
        Objects.requireNonNull(algorithmVersion, "algorithmVersion");
        Objects.requireNonNull(randomProvider, "randomProvider");
        Objects.requireNonNull(generatedAt, "generatedAt");
    }

    public DrawResult(
            UUID id,
            UUID drawId,
            Combination winningCombination,
            String algorithmVersion,
            String randomProvider,
            String proofHash,
            UUID generatedBy,
            Instant generatedAt) {
        this(id, drawId, winningCombination, algorithmVersion, randomProvider, proofHash, generatedBy, generatedAt, null, null);
    }

    public Optional<String> maybeProofHash() {
        return Optional.ofNullable(proofHash);
    }

    public Optional<String> maybeRequestId() {
        return Optional.ofNullable(requestId);
    }

    public Optional<String> maybeCorrelationId() {
        return Optional.ofNullable(correlationId);
    }
}
