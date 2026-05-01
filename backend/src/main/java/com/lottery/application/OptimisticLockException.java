package com.lottery.application;

public final class OptimisticLockException extends ApplicationException {
    public OptimisticLockException(String entityName) {
        super("OPTIMISTIC_LOCK_FAILED", entityName + " was changed concurrently");
    }
}
