package com.lottery.infrastructure.payment;

import com.lottery.application.usecase.payment.ProcessPaymentOutboxUseCase;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaymentOutboxWorker extends AbstractLifeCycle {
    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxWorker.class);

    private final ProcessPaymentOutboxUseCase useCase;
    private final int batchSize;
    private final long intervalSeconds;
    private ScheduledExecutorService executor;

    public PaymentOutboxWorker(ProcessPaymentOutboxUseCase useCase, int batchSize, long intervalSeconds) {
        this.useCase = useCase;
        this.batchSize = batchSize;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    protected void doStart() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "payment-outbox-worker");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::processSafely, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("payment_outbox_worker_started intervalSeconds={} batchSize={}", intervalSeconds, batchSize);
    }

    @Override
    protected void doStop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        log.info("payment_outbox_worker_stopped");
    }

    private void processSafely() {
        try {
            var result = useCase.executeDue(batchSize);
            if (result.processed() > 0 || result.failed() > 0) {
                log.info("payment_outbox_worker_tick processed={} failed={}", result.processed(), result.failed());
            }
        } catch (Exception exception) {
            log.warn("payment_outbox_worker_tick_failed", exception);
        }
    }
}
