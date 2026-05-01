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
        String proofHash,
        UUID generatedBy,
        Instant generatedAt) {
    public DrawResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(drawId, "drawId");
        Objects.requireNonNull(winningCombination, "winningCombination");
        Objects.requireNonNull(algorithmVersion, "algorithmVersion");
        Objects.requireNonNull(generatedAt, "generatedAt");
    }

    public Optional<String> maybeProofHash() {
        return Optional.ofNullable(proofHash);
    }
}
