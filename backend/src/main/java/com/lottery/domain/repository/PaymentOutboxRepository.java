package com.lottery.domain.repository;

import com.lottery.domain.model.PaymentOutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOutboxRepository {
    PaymentOutboxMessage save(PaymentOutboxMessage message);

    PaymentOutboxMessage update(PaymentOutboxMessage message);

    Optional<PaymentOutboxMessage> findById(UUID id);

    List<PaymentOutboxMessage> findDueForProcessing(Instant now, int limit);
}
