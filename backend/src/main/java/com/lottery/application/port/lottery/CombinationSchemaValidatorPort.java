package com.lottery.application.port.lottery;

import com.lottery.domain.model.CombinationSchema;

public interface CombinationSchemaValidatorPort {
    void validateSchema(CombinationSchema schema);
}
