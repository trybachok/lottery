package com.lottery.application;

import java.util.Map;

public class ApplicationException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    public ApplicationException(String code, String message) {
        this(code, message, Map.of());
    }

    public ApplicationException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
