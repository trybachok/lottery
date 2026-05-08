package com.lottery.application.usecase.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.NotFoundException;
import com.lottery.application.dto.PaymentOutboxProcessingResultDto;
import com.lottery.application.port.payment.PaymentProviderPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Payment;
import com.lottery.domain.model.PaymentOutboxMessage;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentOutboxRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.TicketStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProcessPaymentOutboxUseCase {
    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentOutboxUseCase.class);

    private final PaymentOutboxRepository outboxRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final PaymentProviderPort paymentProviderPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final ObjectMapper objectMapper;

    public ProcessPaymentOutboxUseCase(
            PaymentOutboxRepository outboxRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            TicketRepository ticketRepository,
            PaymentProviderPort paymentProviderPort,
            TransactionManager transactionManager,
            DomainClock clock,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.paymentProviderPort = paymentProviderPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public PaymentOutboxProcessingResultDto executeDue(int limit) {
        return transactionManager.inTransaction(() -> {
            Instant now = clock.now();
            int processed = 0;
            int failed = 0;
            for (PaymentOutboxMessage message : outboxRepository.findDueForProcessing(now, limit)) {
                PaymentOutboxMessage processing = outboxRepository.update(message.markProcessing(now));
                try {
                    process(processing);
                    outboxRepository.update(processing.markProcessed(clock.now()));
                    processed++;
                } catch (Exception exception) {
                    failed++;
                    Instant retryAt = clock.now().plusSeconds(retryDelaySeconds(processing.attempts()));
                    outboxRepository.update(processing.markFailed(exception.getMessage(), retryAt, clock.now()));
                    log.warn(
                            "outboxId={} type={} invoiceId={} paymentId={} payment_outbox_failed",
                            processing.id(),
                            processing.type(),
                            processing.invoiceId(),
                            processing.paymentId(),
                            exception);
                }
            }
            return new PaymentOutboxProcessingResultDto(processed, failed);
        });
    }

    private void process(PaymentOutboxMessage message) {
        switch (message.type()) {
            case CREATE_INVOICE -> processCreateInvoice(message);
            case CANCEL_PAYMENT -> processCancelPayment(message);
            case REFUND_PAYMENT -> processRefundPayment(message);
        }
    }

    private void processCreateInvoice(PaymentOutboxMessage message) {
        Invoice invoice = invoiceRepository.findById(message.invoiceId()).orElseThrow(() -> new NotFoundException("Invoice"));
        Payment payment = paymentRepository.findByInvoiceId(invoice.id()).orElseThrow(() -> new NotFoundException("Payment"));
        if (invoice.status() != InvoiceStatus.CREATED) {
            return;
        }
        PaymentProviderPort.InvoiceCreationResult result = paymentProviderPort.createInvoice(new PaymentProviderPort.InvoiceCreationRequest(
                invoice.providerCode(),
                invoice.ticketId(),
                invoice.userId(),
                invoice.amount().amount(),
                invoice.amount().currency(),
                idempotencyKey(message)));
        Instant now = clock.now();
        Invoice updatedInvoice = invoice
                .withProviderData(result.externalInvoiceId(), result.paymentUrl(), now)
                .withStatus(InvoiceStatus.PENDING, now);
        invoiceRepository.update(updatedInvoice);
        paymentRepository.update(payment.withExternalPaymentId(result.externalPaymentId(), now));
        log.info("outboxId={} invoiceId={} paymentId={} provider_invoice_created", message.id(), invoice.id(), payment.id());
    }

    private void processCancelPayment(PaymentOutboxMessage message) {
        Payment payment = paymentRepository.findById(message.paymentId()).orElseThrow(() -> new NotFoundException("Payment"));
        if (payment.maybeExternalPaymentId().isEmpty()) {
            return;
        }
        paymentProviderPort.cancelPayment(new PaymentProviderPort.PaymentCancellationRequest(
                payment.providerCode(),
                payment.maybeExternalPaymentId().orElseThrow(),
                idempotencyKey(message)));
        log.info("outboxId={} paymentId={} provider_payment_cancelled", message.id(), payment.id());
    }

    private void processRefundPayment(PaymentOutboxMessage message) {
        Payment payment = paymentRepository.findById(message.paymentId()).orElseThrow(() -> new NotFoundException("Payment"));
        Invoice invoice = invoiceRepository.findById(payment.invoiceId()).orElseThrow(() -> new NotFoundException("Invoice"));
        Ticket ticket = ticketRepository.findById(invoice.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
        PaymentProviderPort.RefundResult result = paymentProviderPort.refundPayment(new PaymentProviderPort.RefundRequest(
                payment.providerCode(),
                payment.maybeExternalPaymentId().orElseThrow(),
                payment.amount().amount(),
                payment.amount().currency(),
                idempotencyKey(message)));
        Instant now = clock.now();
        invoiceRepository.update(invoice.withStatus(InvoiceStatus.REFUNDED, now));
        paymentRepository.update(payment.withStatus(PaymentStatus.REFUNDED, now));
        ticketRepository.update(ticket.withPaymentStatus(TicketStatus.REFUNDED, now));
        log.info("outboxId={} paymentId={} externalRefundId={} provider_payment_refunded", message.id(), payment.id(), result.externalRefundId());
    }

    private String idempotencyKey(PaymentOutboxMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.payloadJson());
            String value = root.path("idempotencyKey").asText();
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey is required in outbox payload");
            }
            return value;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid payment outbox payload", exception);
        }
    }

    private long retryDelaySeconds(int attempts) {
        int nextAttemptNumber = attempts + 1;
        return Math.min(3600, Math.max(30, 30L * nextAttemptNumber));
    }
}
