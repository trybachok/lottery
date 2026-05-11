package com.lottery.application.usecase.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.ProcessPaymentWebhookCommand;
import com.lottery.application.command.SimulateMockPaymentWebhookCommand;
import com.lottery.application.dto.PaymentWebhookResultDto;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.payment.WebhookSignatureGeneratorPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimulateMockPaymentWebhookUseCase {
    private static final Logger log = LoggerFactory.getLogger(SimulateMockPaymentWebhookUseCase.class);
    private static final String MOCK_PROVIDER_CODE = "mock";
    private static final Set<String> SUPPORTED_EVENTS = Set.of("PAYMENT_SUCCEEDED", "PAYMENT_FAILED");

    private final InvoiceRepository invoiceRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final ProcessPaymentWebhookUseCase processPaymentWebhookUseCase;
    private final WebhookSignatureGeneratorPort signatureGenerator;
    private final DomainClock clock;
    private final ObjectMapper objectMapper;

    public SimulateMockPaymentWebhookUseCase(
            InvoiceRepository invoiceRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            ProcessPaymentWebhookUseCase processPaymentWebhookUseCase,
            WebhookSignatureGeneratorPort signatureGenerator,
            DomainClock clock,
            ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.processPaymentWebhookUseCase = processPaymentWebhookUseCase;
        this.signatureGenerator = signatureGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public PaymentWebhookResultDto execute(SimulateMockPaymentWebhookCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PAYMENT_READ);
            if (!SUPPORTED_EVENTS.contains(command.eventType())) {
                throw new ConflictException("MOCK_PAYMENT_EVENT_UNSUPPORTED", "Unsupported mock payment event");
            }
            Invoice invoice = invoiceRepository.findById(command.invoiceId()).orElseThrow(() -> new NotFoundException("Invoice"));
            InvoiceAccess.ensureCanAccess(invoice, context, authorizationPort);
            if (!MOCK_PROVIDER_CODE.equals(invoice.providerCode())) {
                throw new ConflictException("MOCK_PAYMENT_PROVIDER_REQUIRED", "Invoice provider is not mock");
            }
            if (invoice.status() != InvoiceStatus.PENDING) {
                throw new ConflictException("MOCK_INVOICE_NOT_PENDING", "Mock invoice must be PENDING");
            }
            String externalInvoiceId = invoice.maybeExternalInvoiceId()
                    .orElseThrow(() -> new ConflictException("MOCK_INVOICE_NOT_READY", "Mock invoice is not ready yet"));
            String payload = payload(command.eventType(), externalInvoiceId);
            String signature = signatureGenerator.sign(invoice.providerCode(), payload);
            log.info(
                    "requestId={} invoiceId={} eventType={} mock_payment_webhook_simulated",
                    context.requestId(),
                    invoice.id(),
                    command.eventType());
            return processPaymentWebhookUseCase.execute(new ProcessPaymentWebhookCommand(invoice.providerCode(), payload, signature));
        });
    }

    private String payload(String eventType, String externalInvoiceId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", eventId(eventType, externalInvoiceId),
                    "eventType", eventType,
                    "externalInvoiceId", externalInvoiceId));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build mock payment webhook payload", exception);
        }
    }

    private String eventId(String eventType, String externalInvoiceId) {
        return "evt-ui-%s-%s-%s-%s".formatted(
                eventType,
                externalInvoiceId,
                clock.now().toEpochMilli(),
                UUID.randomUUID());
    }
}
