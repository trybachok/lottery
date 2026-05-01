package com.lottery.domain.repository;

import com.lottery.domain.model.PaymentWebhookEvent;
import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookEventRepository {
    void acquireProcessingLock(String providerCode, String externalEventId);

    PaymentWebhookEvent save(PaymentWebhookEvent event);

    PaymentWebhookEvent update(PaymentWebhookEvent event);

    Optional<PaymentWebhookEvent> findProcessedByProviderAndExternalEventId(String providerCode, String externalEventId);

    Optional<PaymentWebhookEvent> findById(UUID id);
}
