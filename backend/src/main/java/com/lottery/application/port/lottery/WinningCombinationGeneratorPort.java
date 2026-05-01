package com.lottery.application.port.lottery;

import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.valueobject.Combination;

public interface WinningCombinationGeneratorPort {
    GeneratedWinningCombination generate(CombinationSchema schema);

    record GeneratedWinningCombination(
            Combination combination,
            String algorithmVersion,
            String randomProvider,
            String proofHash) {
    }
}
