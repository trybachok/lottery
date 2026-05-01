package com.lottery.application.port.lottery;

import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.valueobject.Combination;
import java.math.BigDecimal;

public interface CombinationEvaluatorPort {
    BigDecimal matchPercent(Combination ticketCombination, Combination winningCombination, CombinationSchema schema);
}
