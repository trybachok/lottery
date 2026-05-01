package com.lottery.presentation.error;

import java.util.Map;

public record ErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, Map<String, Object> details, String requestId) {
        public ErrorBody {
            details = details == null ? Map.of() : Map.copyOf(details);
        }
    }
}
