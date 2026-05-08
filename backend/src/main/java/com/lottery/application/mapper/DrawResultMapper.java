package com.lottery.application.mapper;

import com.lottery.application.dto.DrawResultDto;
import com.lottery.domain.model.DrawResult;

public final class DrawResultMapper {
    public DrawResultDto toDto(DrawResult result) {
        return new DrawResultDto(
                result.id(),
                result.drawId(),
                result.winningCombination().values(),
                result.algorithmVersion(),
                result.randomProvider(),
                result.maybeProofHash().orElse(null),
                result.generatedBy(),
                result.generatedAt(),
                result.maybeRequestId().orElse(null),
                result.maybeCorrelationId().orElse(null));
    }
}
