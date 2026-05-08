package com.lottery.domain.repository;

import com.lottery.domain.model.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Invoice update(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    Optional<Invoice> findByExternalInvoiceId(String providerCode, String externalInvoiceId);

    Optional<Invoice> findActiveByTicketId(UUID ticketId);

    default List<Invoice> findByTicketId(UUID ticketId) {
        return List.of();
    }

    List<Invoice> findByUserId(UUID userId, int limit, int offset);
}
