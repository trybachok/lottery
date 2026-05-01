package com.lottery.application;

public final class ForbiddenException extends ApplicationException {
    public ForbiddenException(String permission) {
        super("FORBIDDEN", "Required permission is missing: " + permission);
    }
}
