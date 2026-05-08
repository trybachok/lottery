package com.lottery.application.usecase.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.command.ProcessPaymentWebhookCommand;
import com.lottery.application.dto.PaymentWebhookResultDto;
import com.lottery.application.port.payment.WebhookSignatureVerifierPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Payment;
import com.lottery.domain.model.PaymentWebhookEvent;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.PaymentWebhookEventRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.TicketStatus;
import java.time.Instant;

public final class ProcessPaymentWebhookUseCase {
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final WebhookSignatureVerifierPort signatureVerifier;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final ObjectMapper objectMapper;

    public ProcessPaymentWebhookUseCase(
            PaymentWebhookEventRepository webhookEventRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            TicketRepository ticketRepository,
            WebhookSignatureVerifierPort signatureVerifier,
            TransactionManager transactionManager,
            DomainClock clock,
            ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.signatureVerifier = signatureVerifier;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public PaymentWebhookResultDto execute(ProcessPaymentWebhookCommand command) {
        return transactionManager.inTransaction(() -> {
            WebhookPayload payload = parse(command.payloadJson());
            webhookEventRepository.acquireProcessingLock(command.providerCode(), payload.externalEventId());
            PaymentWebhookEvent duplicate = webhookEventRepository
                    .findProcessedByProviderAndExternalEventId(command.providerCode(), payload.externalEventId())
                    .orElse(null);
            boolean signatureValid = signatureVerifier.isValid(command.providerCode(), command.payloadJson(), command.signature());
            PaymentWebhookEvent event = webhookEventRepository.save(new PaymentWebhookEvent(
                    DomainIds.newId(),
                    command.providerCode(),
                    payload.eventType(),
                    payload.externalEventId(),
                    command.payloadJson(),
                    signatureValid,
                    false,
                    clock.now()));
            if (!signatureValid) {
                return new PaymentWebhookResultDto(event.id(), false, false, "SIGNATURE_INVALID");
            }
            if (duplicate != null) {
                webhookEventRepository.update(event.markProcessed());
                return new PaymentWebhookResultDto(event.id(), true, true, "DUPLICATE");
            }
            Invoice invoice = invoiceRepository
                    .findByExternalInvoiceId(command.providerCode(), payload.externalInvoiceId())
                    .orElseThrow(() -> new NotFoundException("Invoice"));
            Payment payment = paymentRepository.findByInvoiceId(invoice.id()).orElseThrow(() -> new NotFoundException("Payment"));
            Ticket ticket = ticketRepository.findById(invoice.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
            Instant now = clock.now();
            switch (payload.eventType()) {
                case "PAYMENT_AUTHORIZED" -> {
                    invoiceRepository.update(invoice.withStatus(InvoiceStatus.PENDING, now));
                    paymentRepository.update(payment.withStatus(PaymentStatus.AUTHORIZED, now));
                    ticketRepository.update(ticket.withPaymentStatus(TicketStatus.PAYMENT_PENDING, now));
                }
                case "PAYMENT_SUCCEEDED" -> {
                    invoiceRepository.update(invoice.withStatus(InvoiceStatus.PAID, now));
                    paymentRepository.update(payment.withStatus(PaymentStatus.CAPTURED, now));
                    ticketRepository.update(ticket.withPaymentStatus(TicketStatus.PAID, now));
                }
                case "PAYMENT_FAILED" -> {
                    invoiceRepository.update(invoice.withStatus(InvoiceStatus.FAILED, now));
                    paymentRepository.update(payment.withStatus(PaymentStatus.FAILED, now));
                    ticketRepository.update(ticket.withPaymentStatus(TicketStatus.PAYMENT_FAILED, now));
                }
                case "PAYMENT_CANCELLED" -> {
                    invoiceRepository.update(invoice.withStatus(InvoiceStatus.CANCELLED, now));
                    paymentRepository.update(payment.withStatus(PaymentStatus.CANCELLED, now));
                    ticketRepository.update(ticket.withPaymentReleased(now));
                }
                case "PAYMENT_EXPIRED" -> {
                    invoiceRepository.update(invoice.withStatus(InvoiceStatus.EXPIRED, now));
                    paymentRepository.update(payment.withStatus(PaymentStatus.CANCELLED, now));
                    ticketRepository.update(ticket.withPaymentReleased(now));
                }
                default -> throw new ConflictException("PAYMENT_WEBHOOK_UNSUPPORTED_EVENT", "Unsupported payment webhook event");
            }
            webhookEventRepository.update(event.markProcessed());
            return new PaymentWebhookResultDto(event.id(), true, false, payload.eventType());
        });
    }

    private WebhookPayload parse(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            return new WebhookPayload(
                    root.path("eventId").asText(),
                    root.path("eventType").asText(),
                    root.path("externalInvoiceId").asText());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid payment webhook payload", exception);
        }
    }

    private record WebhookPayload(String externalEventId, String eventType, String externalInvoiceId) {
        private WebhookPayload {
            if (externalEventId == null || externalEventId.isBlank()
                    || eventType == null || eventType.isBlank()
                    || externalInvoiceId == null || externalInvoiceId.isBlank()) {
                throw new IllegalArgumentException("Payment webhook eventId, eventType and externalInvoiceId are required");
            }
        }
    }
}
