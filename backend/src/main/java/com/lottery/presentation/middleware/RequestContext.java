package com.lottery.presentation.middleware;

public record RequestContext(String requestId, String correlationId, long startedAtNanos) {
    public static final String ATTRIBUTE_NAME = RequestContext.class.getName();
}
