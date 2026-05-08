package com.lottery.application.port.lottery;

import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.valueobject.Combination;

public interface CombinationValidatorPort {
    void validate(Combination combination, CombinationSchema schema);
}

