package com.lottery.domain.repository;

import com.lottery.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);

    Payment update(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    Optional<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByInvoiceId(UUID invoiceId, int limit, int offset);
}
