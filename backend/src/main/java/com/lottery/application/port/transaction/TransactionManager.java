package com.lottery.application.port.transaction;

public interface TransactionManager {
    <T> T inTransaction(TransactionalWork<T> work);

    @FunctionalInterface
    interface TransactionalWork<T> {
        T execute();
    }
}
