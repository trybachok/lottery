package com.lottery.application;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
