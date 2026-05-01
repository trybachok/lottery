package com.lottery.application;

public final class ConflictException extends ApplicationException {
    public ConflictException(String code, String message) {
        super(code, message);
    }
}
