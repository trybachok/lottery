package com.lottery.application;

public final class NotFoundException extends ApplicationException {
    public NotFoundException(String entityName) {
        super("NOT_FOUND", entityName + " was not found");
    }
}
