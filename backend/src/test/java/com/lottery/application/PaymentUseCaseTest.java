package com.lottery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CancelInvoiceCommand;
import com.lottery.application.command.CreateInvoiceForTicketCommand;
import com.lottery.application.command.ProcessPaymentWebhookCommand;
import com.lottery.application.command.RefundPaymentCommand;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.mapper.PaymentMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.payment.PaymentProviderPort;
import com.lottery.application.port.payment.WebhookSignatureVerifierPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.usecase.payment.CancelInvoiceUseCase;
import com.lottery.application.usecase.payment.CreateInvoiceForTicketUseCase;
import com.lottery.application.usecase.payment.ExpireInvoiceUseCase;
import com.lottery.application.usecase.payment.GetInvoiceUseCase;
import com.lottery.application.usecase.payment.GetTicketInvoiceUseCase;
import com.lottery.application.usecase.payment.ProcessPaymentOutboxUseCase;
import com.lottery.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.lottery.application.usecase.payment.RefundPaymentUseCase;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Payment;
import com.lottery.domain.model.PaymentOutboxMessage;
import com.lottery.domain.model.PaymentWebhookEvent;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentOutboxRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.PaymentWebhookEventRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.PaymentOutboxStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PaymentUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");
    private static final Currency RUB = Currency.getInstance("RUB");

    @Test
    void createInvoiceCreatesProviderInvoicePaymentAndMarksTicketPending() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = ticket(userId, TicketStatus.CREATED);
        InMemoryTicketRepository tickets = new InMemoryTicketRepository(List.of(ticket));
        InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
        InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        FakePaymentProvider provider = new FakePaymentProvider();
        CreateInvoiceForTicketUseCase useCase = new CreateInvoiceForTicketUseCase(
                tickets,
                invoices,
                payments,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new InvoiceMapper());

        var result = useCase.execute(
                new CreateInvoiceForTicketCommand(ticket.id(), "mock", "invoice-idem-1"),
                new UseCaseContext(userId, Set.of(PermissionCodes.TICKET_CREATE), "req_test"));

        assertEquals("CREATED", result.status());
        assertEquals("mock", result.providerCode());
        assertEquals(null, result.paymentUrl());
        assertEquals(null, provider.lastInvoiceRequest);
        assertEquals(InvoiceStatus.CREATED, invoices.byId.get(result.id()).status());
        assertEquals(PaymentStatus.INITIATED, payments.findByInvoiceId(result.id()).orElseThrow().status());
        assertEquals(TicketStatus.PAYMENT_PENDING, tickets.byId.get(ticket.id()).status());
        assertEquals(1, outbox.byId.size());
    }

    @Test
    void paymentOutboxCreatesProviderInvoiceAndUpdatesExternalFields() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = ticket(userId, TicketStatus.CREATED);
        InMemoryTicketRepository tickets = new InMemoryTicketRepository(List.of(ticket));
        InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
        InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        FakePaymentProvider provider = new FakePaymentProvider();
        CreateInvoiceForTicketUseCase createUseCase = new CreateInvoiceForTicketUseCase(
                tickets,
                invoices,
                payments,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new InvoiceMapper());

        var invoiceDto = createUseCase.execute(
                new CreateInvoiceForTicketCommand(ticket.id(), "mock", "invoice-idem-1"),
                new UseCaseContext(userId, Set.of(PermissionCodes.TICKET_CREATE), "req_test"));
        ProcessPaymentOutboxUseCase outboxUseCase = new ProcessPaymentOutboxUseCase(
                outbox,
                invoices,
                payments,
                tickets,
                provider,
                directTransaction(),
                fixedClock(),
                new ObjectMapper());

        var result = outboxUseCase.executeDue(10);

        assertEquals(1, result.processed());
        assertEquals(0, result.failed());
        assertEquals("mock", provider.lastInvoiceRequest.providerCode());
        assertEquals(InvoiceStatus.PENDING, invoices.byId.get(invoiceDto.id()).status());
        assertEquals("ext-inv-" + ticket.id(), invoices.byId.get(invoiceDto.id()).maybeExternalInvoiceId().orElseThrow());
        assertEquals("https://mock-payments.test/invoices/ext-inv-" + ticket.id(), invoices.byId.get(invoiceDto.id()).maybePaymentUrl().orElseThrow());
        assertEquals("ext-pay-" + ticket.id(), payments.findByInvoiceId(invoiceDto.id()).orElseThrow().maybeExternalPaymentId().orElseThrow());
        assertEquals(PaymentOutboxStatus.PROCESSED, outbox.byId.values().iterator().next().status());
    }

    @Test
    void createInvoiceRejectsAnotherActiveInvoiceForTicketWithDifferentIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = ticket(userId, TicketStatus.PAYMENT_PENDING);
        Invoice invoice = invoice(ticket, InvoiceStatus.PENDING);
        InMemoryTicketRepository tickets = new InMemoryTicketRepository(List.of(ticket));
        InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository(List.of(invoice));
        InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        CreateInvoiceForTicketUseCase useCase = new CreateInvoiceForTicketUseCase(
                tickets,
                invoices,
                payments,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new InvoiceMapper());

        ConflictException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        new CreateInvoiceForTicketCommand(ticket.id(), "mock", "another-idem"),
                        new UseCaseContext(userId, Set.of(PermissionCodes.TICKET_CREATE), "req_test")));

        assertEquals("ACTIVE_INVOICE_EXISTS", exception.code());
    }

    @Test
    void successfulWebhookCapturesPaymentMarksTicketPaidAndIgnoresDuplicateBusinessProcessing() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        ProcessPaymentWebhookUseCase useCase = fixture.webhookUseCase(true);
        String payload = webhookPayload("evt_1", "PAYMENT_SUCCEEDED", fixture.invoice.externalInvoiceId());

        var result = useCase.execute(new ProcessPaymentWebhookCommand("mock", payload, "valid"));

        assertTrue(result.processed());
        assertFalse(result.duplicate());
        assertEquals(InvoiceStatus.PAID, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(PaymentStatus.CAPTURED, fixture.payments.byId.get(fixture.payment.id()).status());
        assertEquals(TicketStatus.PAID, fixture.tickets.byId.get(fixture.ticket.id()).status());
        int invoiceUpdatesAfterFirstDelivery = fixture.invoices.updateCount;
        int paymentUpdatesAfterFirstDelivery = fixture.payments.updateCount;
        int ticketUpdatesAfterFirstDelivery = fixture.tickets.updateCount;

        var duplicate = useCase.execute(new ProcessPaymentWebhookCommand("mock", payload, "valid"));

        assertTrue(duplicate.processed());
        assertTrue(duplicate.duplicate());
        assertEquals(invoiceUpdatesAfterFirstDelivery, fixture.invoices.updateCount);
        assertEquals(paymentUpdatesAfterFirstDelivery, fixture.payments.updateCount);
        assertEquals(ticketUpdatesAfterFirstDelivery, fixture.tickets.updateCount);
    }

    @Test
    void failedWebhookMarksInvoicePaymentAndTicketFailed() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        ProcessPaymentWebhookUseCase useCase = fixture.webhookUseCase(true);

        useCase.execute(new ProcessPaymentWebhookCommand(
                "mock",
                webhookPayload("evt_2", "PAYMENT_FAILED", fixture.invoice.externalInvoiceId()),
                "valid"));

        assertEquals(InvoiceStatus.FAILED, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(PaymentStatus.FAILED, fixture.payments.byId.get(fixture.payment.id()).status());
        assertEquals(TicketStatus.PAYMENT_FAILED, fixture.tickets.byId.get(fixture.ticket.id()).status());
    }

    @Test
    void invalidWebhookSignatureIsStoredButNotProcessed() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        ProcessPaymentWebhookUseCase useCase = fixture.webhookUseCase(false);

        var result = useCase.execute(new ProcessPaymentWebhookCommand(
                "mock",
                webhookPayload("evt_bad", "PAYMENT_SUCCEEDED", fixture.invoice.externalInvoiceId()),
                "invalid"));

        assertFalse(result.processed());
        assertEquals("SIGNATURE_INVALID", result.status());
        assertEquals(1, fixture.webhookEvents.byId.size());
        PaymentWebhookEvent event = fixture.webhookEvents.byId.get(result.eventId());
        assertFalse(event.signatureValid());
        assertFalse(event.processed());
        assertEquals(InvoiceStatus.PENDING, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(PaymentStatus.INITIATED, fixture.payments.byId.get(fixture.payment.id()).status());
        assertEquals(TicketStatus.PAYMENT_PENDING, fixture.tickets.byId.get(fixture.ticket.id()).status());
    }

    @Test
    void getInvoiceEnforcesOwnership() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        GetInvoiceUseCase useCase = new GetInvoiceUseCase(
                fixture.invoices,
                grantAll(),
                directTransaction(),
                new InvoiceMapper());

        var result = useCase.execute(
                fixture.invoice.id(),
                new UseCaseContext(fixture.ticket.userId(), Set.of(PermissionCodes.PAYMENT_READ), "req_test"));

        assertEquals(fixture.invoice.id(), result.id());
        org.junit.jupiter.api.Assertions.assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        fixture.invoice.id(),
                        new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.PAYMENT_READ), "req_test")));
    }

    @Test
    void getTicketInvoiceReturnsLatestInvoiceAndEnforcesOwnership() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        Invoice olderInvoice = new Invoice(
                UUID.randomUUID(),
                fixture.ticket.id(),
                fixture.ticket.userId(),
                "mock",
                InvoiceStatus.EXPIRED,
                fixture.ticket.price(),
                "ext-inv-old",
                "invoice-idem-old",
                NOW.minusSeconds(300),
                NOW.minusSeconds(60),
                null);
        fixture.invoices.save(olderInvoice);
        GetTicketInvoiceUseCase useCase = new GetTicketInvoiceUseCase(
                fixture.tickets,
                fixture.invoices,
                grantAll(),
                directTransaction(),
                new InvoiceMapper());

        var result = useCase.execute(
                fixture.ticket.id(),
                new UseCaseContext(fixture.ticket.userId(), Set.of(PermissionCodes.PAYMENT_READ), "req_test"));

        assertEquals(fixture.invoice.id(), result.id());
        assertEquals(fixture.ticket.id(), result.ticketId());
        org.junit.jupiter.api.Assertions.assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        fixture.ticket.id(),
                        new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.PAYMENT_READ), "req_test")));
    }

    @Test
    void getTicketInvoiceReturnsNotFoundWhenTicketHasNoInvoice() {
        Ticket ticket = ticket(UUID.randomUUID(), TicketStatus.CREATED);
        GetTicketInvoiceUseCase useCase = new GetTicketInvoiceUseCase(
                new InMemoryTicketRepository(List.of(ticket)),
                new InMemoryInvoiceRepository(),
                grantAll(),
                directTransaction(),
                new InvoiceMapper());

        org.junit.jupiter.api.Assertions.assertThrows(
                NotFoundException.class,
                () -> useCase.execute(
                        ticket.id(),
                        new UseCaseContext(ticket.userId(), Set.of(PermissionCodes.PAYMENT_READ), "req_test")));
    }

    @Test
    void cancelInvoiceClosesActiveInvoiceReleasesTicketAndEnqueuesProviderCancel() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        CancelInvoiceUseCase useCase = new CancelInvoiceUseCase(
                fixture.invoices,
                fixture.payments,
                fixture.tickets,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new InvoiceMapper());

        var result = useCase.execute(
                new CancelInvoiceCommand(fixture.invoice.id(), "cancel-idem-1"),
                new UseCaseContext(fixture.ticket.userId(), Set.of(PermissionCodes.PAYMENT_READ), "req_test"));

        assertEquals("CANCELLED", result.status());
        assertEquals(InvoiceStatus.CANCELLED, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(PaymentStatus.CANCELLED, fixture.payments.byId.get(fixture.payment.id()).status());
        assertEquals(TicketStatus.CREATED, fixture.tickets.byId.get(fixture.ticket.id()).status());
        assertEquals(1, outbox.byId.size());
    }

    @Test
    void expireInvoiceRequiresExpiredDeadline() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAYMENT_PENDING, InvoiceStatus.PENDING, PaymentStatus.INITIATED);
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        ExpireInvoiceUseCase useCase = new ExpireInvoiceUseCase(new CancelInvoiceUseCase(
                fixture.invoices,
                fixture.payments,
                fixture.tickets,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new InvoiceMapper()));

        ConflictException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ConflictException.class,
                () -> useCase.execute(
                        fixture.invoice.id(),
                        new UseCaseContext(fixture.ticket.userId(), Set.of(PermissionCodes.PAYMENT_READ), "req_test")));

        assertEquals("INVOICE_NOT_EXPIRED", exception.code());
    }

    @Test
    void refundCapturedPaymentEnqueuesOutboxAndProcessorMovesInvoicePaymentAndTicketToRefunded() {
        PaymentFixture fixture = new PaymentFixture(TicketStatus.PAID, InvoiceStatus.PAID, PaymentStatus.CAPTURED);
        FakePaymentProvider provider = new FakePaymentProvider();
        InMemoryPaymentOutboxRepository outbox = new InMemoryPaymentOutboxRepository();
        RefundPaymentUseCase useCase = new RefundPaymentUseCase(
                fixture.payments,
                fixture.invoices,
                fixture.tickets,
                outbox,
                grantAll(),
                directTransaction(),
                fixedClock(),
                new PaymentMapper());

        var result = useCase.execute(
                new RefundPaymentCommand(fixture.payment.id(), "refund-idem-1"),
                new UseCaseContext(UUID.randomUUID(), Set.of(PermissionCodes.PAYMENT_REFUND), "req_test"));

        assertEquals("CAPTURED", result.status());
        assertEquals(0, provider.refundCalls);
        assertEquals(InvoiceStatus.REFUND_PENDING, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(TicketStatus.REFUND_PENDING, fixture.tickets.byId.get(fixture.ticket.id()).status());
        assertEquals(1, outbox.byId.size());

        ProcessPaymentOutboxUseCase outboxUseCase = new ProcessPaymentOutboxUseCase(
                outbox,
                fixture.invoices,
                fixture.payments,
                fixture.tickets,
                provider,
                directTransaction(),
                fixedClock(),
                new ObjectMapper());
        outboxUseCase.executeDue(10);

        assertEquals(1, provider.refundCalls);
        assertEquals(InvoiceStatus.REFUNDED, fixture.invoices.byId.get(fixture.invoice.id()).status());
        assertEquals(PaymentStatus.REFUNDED, fixture.payments.byId.get(fixture.payment.id()).status());
        assertEquals(TicketStatus.REFUNDED, fixture.tickets.byId.get(fixture.ticket.id()).status());
    }

    private static String webhookPayload(String eventId, String eventType, String externalInvoiceId) {
        return """
                {"eventId":"%s","eventType":"%s","externalInvoiceId":"%s"}
                """.formatted(eventId, eventType, externalInvoiceId).trim();
    }

    private static Ticket ticket(UUID userId, TicketStatus status) {
        return new Ticket(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                status,
                new Combination(List.of("1", "2", "3")),
                new Money(BigDecimal.valueOf(100), RUB),
                null,
                null,
                false,
                NOW.minusSeconds(60),
                status == TicketStatus.PAID ? NOW.minusSeconds(30) : null,
                null,
                null,
                null,
                0);
    }

    private static Invoice invoice(Ticket ticket, InvoiceStatus status) {
        return new Invoice(
                UUID.randomUUID(),
                ticket.id(),
                ticket.userId(),
                "mock",
                status,
                ticket.price(),
                "ext-inv-" + ticket.id(),
                "https://mock-payments.test/invoices/ext-inv-" + ticket.id(),
                "invoice-idem-fixture",
                NOW.minusSeconds(30),
                NOW.plusSeconds(900),
                status == InvoiceStatus.PAID ? NOW.minusSeconds(20) : null);
    }

    private static AuthorizationPort grantAll() {
        return new AuthorizationPort() {
            @Override
            public void ensurePermission(UseCaseContext context, String permissionCode) {
            }

            @Override
            public boolean hasRole(UseCaseContext context, String roleCode) {
                return false;
            }
        };
    }

    private static TransactionManager directTransaction() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(TransactionalWork<T> work) {
                return work.execute();
            }
        };
    }

    private static DomainClock fixedClock() {
        return () -> NOW;
    }

    private static final class PaymentFixture {
        private final Ticket ticket;
        private final Invoice invoice;
        private final Payment payment;
        private final InMemoryTicketRepository tickets;
        private final InMemoryInvoiceRepository invoices;
        private final InMemoryPaymentRepository payments;
        private final InMemoryPaymentWebhookEventRepository webhookEvents = new InMemoryPaymentWebhookEventRepository();

        private PaymentFixture(TicketStatus ticketStatus, InvoiceStatus invoiceStatus, PaymentStatus paymentStatus) {
            ticket = ticket(UUID.randomUUID(), ticketStatus);
            invoice = new Invoice(
                    UUID.randomUUID(),
                    ticket.id(),
                    ticket.userId(),
                    "mock",
                    invoiceStatus,
                    ticket.price(),
                    "ext-inv-" + ticket.id(),
                    "invoice-idem-fixture",
                    NOW.minusSeconds(30),
                    NOW.plusSeconds(900),
                    invoiceStatus == InvoiceStatus.PAID ? NOW.minusSeconds(20) : null);
            payment = new Payment(
                    UUID.randomUUID(),
                    invoice.id(),
                    "mock",
                    paymentStatus,
                    invoice.amount(),
                    "ext-pay-" + ticket.id(),
                    NOW.minusSeconds(30),
                    NOW.minusSeconds(30));
            tickets = new InMemoryTicketRepository(List.of(ticket));
            invoices = new InMemoryInvoiceRepository(List.of(invoice));
            payments = new InMemoryPaymentRepository(List.of(payment));
        }

        private ProcessPaymentWebhookUseCase webhookUseCase(boolean signatureValid) {
            return new ProcessPaymentWebhookUseCase(
                    webhookEvents,
                    invoices,
                    payments,
                    tickets,
                    signatureVerifier(signatureValid),
                    directTransaction(),
                    fixedClock(),
                    new ObjectMapper());
        }
    }

    private static WebhookSignatureVerifierPort signatureVerifier(boolean valid) {
        return (providerCode, payload, signature) -> valid;
    }

    private static final class FakePaymentProvider implements PaymentProviderPort {
        private InvoiceCreationRequest lastInvoiceRequest;
        private RefundRequest lastRefundRequest;
        private int refundCalls;

        @Override
        public InvoiceCreationResult createInvoice(InvoiceCreationRequest request) {
            lastInvoiceRequest = request;
            String externalInvoiceId = "ext-inv-" + request.ticketId();
            return new InvoiceCreationResult(
                    request.providerCode(),
                    externalInvoiceId,
                    "ext-pay-" + request.ticketId(),
                    "https://mock-payments.test/invoices/" + externalInvoiceId);
        }

        @Override
        public PaymentStatusResult checkStatus(PaymentStatusRequest request) {
            return new PaymentStatusResult("INITIATED", false);
        }

        @Override
        public void cancelPayment(PaymentCancellationRequest request) {
        }

        @Override
        public RefundResult refundPayment(RefundRequest request) {
            refundCalls++;
            lastRefundRequest = request;
            return new RefundResult("ext-refund-" + request.externalPaymentId(), "REFUNDED");
        }
    }

    private static final class InMemoryTicketRepository implements TicketRepository {
        private final Map<UUID, Ticket> byId = new HashMap<>();
        private int updateCount;

        private InMemoryTicketRepository(List<Ticket> tickets) {
            for (Ticket ticket : tickets) {
                byId.put(ticket.id(), ticket);
            }
        }

        @Override
        public Ticket save(Ticket ticket) {
            byId.put(ticket.id(), ticket);
            return ticket;
        }

        @Override
        public Ticket update(Ticket ticket) {
            updateCount++;
            Ticket updated = new Ticket(
                    ticket.id(),
                    ticket.userId(),
                    ticket.drawId(),
                    ticket.status(),
                    ticket.combination(),
                    ticket.price(),
                    ticket.matchPercent().orElse(null),
                    ticket.prizeId().orElse(null),
                    ticket.test(),
                    ticket.createdAt(),
                    ticket.paidAt().orElse(null),
                    ticket.checkedAt().orElse(null),
                    ticket.cancelledAt().orElse(null),
                    ticket.deletedAt().orElse(null),
                    ticket.version() + 1);
            byId.put(updated.id(), updated);
            return updated;
        }

        @Override
        public Optional<Ticket> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Ticket> findAll(int limit, int offset) {
            return byId.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<Ticket> findByUserId(UUID userId, int limit, int offset) {
            return byId.values().stream()
                    .filter(ticket -> ticket.userId().equals(userId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<Ticket> findPaidByDrawId(UUID drawId) {
            return byId.values().stream()
                    .filter(ticket -> ticket.drawId().equals(drawId))
                    .filter(ticket -> ticket.status() == TicketStatus.PAID)
                    .toList();
        }
    }

    private static final class InMemoryInvoiceRepository implements InvoiceRepository {
        private final Map<UUID, Invoice> byId = new HashMap<>();
        private int updateCount;

        private InMemoryInvoiceRepository() {
        }

        private InMemoryInvoiceRepository(List<Invoice> invoices) {
            for (Invoice invoice : invoices) {
                byId.put(invoice.id(), invoice);
            }
        }

        @Override
        public Invoice save(Invoice invoice) {
            byId.put(invoice.id(), invoice);
            return invoice;
        }

        @Override
        public Invoice update(Invoice invoice) {
            updateCount++;
            byId.put(invoice.id(), invoice);
            return invoice;
        }

        @Override
        public Optional<Invoice> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Invoice> findByIdempotencyKey(String idempotencyKey) {
            return byId.values().stream()
                    .filter(invoice -> invoice.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<Invoice> findByExternalInvoiceId(String providerCode, String externalInvoiceId) {
            return byId.values().stream()
                    .filter(invoice -> invoice.providerCode().equals(providerCode))
                    .filter(invoice -> invoice.maybeExternalInvoiceId().orElse("").equals(externalInvoiceId))
                    .findFirst();
        }

        @Override
        public Optional<Invoice> findActiveByTicketId(UUID ticketId) {
            return byId.values().stream()
                    .filter(invoice -> invoice.ticketId().equals(ticketId))
                    .filter(invoice -> invoice.status() == InvoiceStatus.CREATED || invoice.status() == InvoiceStatus.PENDING)
                    .findFirst();
        }

        @Override
        public List<Invoice> findByTicketId(UUID ticketId) {
            return byId.values().stream()
                    .filter(invoice -> invoice.ticketId().equals(ticketId))
                    .toList();
        }

        @Override
        public List<Invoice> findByUserId(UUID userId, int limit, int offset) {
            return byId.values().stream()
                    .filter(invoice -> invoice.userId().equals(userId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryPaymentRepository implements PaymentRepository {
        private final Map<UUID, Payment> byId = new HashMap<>();
        private int updateCount;

        private InMemoryPaymentRepository() {
        }

        private InMemoryPaymentRepository(List<Payment> payments) {
            for (Payment payment : payments) {
                byId.put(payment.id(), payment);
            }
        }

        @Override
        public Payment save(Payment payment) {
            byId.put(payment.id(), payment);
            return payment;
        }

        @Override
        public Payment update(Payment payment) {
            updateCount++;
            byId.put(payment.id(), payment);
            return payment;
        }

        @Override
        public Optional<Payment> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Payment> findByExternalPaymentId(String externalPaymentId) {
            return byId.values().stream()
                    .filter(payment -> payment.maybeExternalPaymentId().orElse("").equals(externalPaymentId))
                    .findFirst();
        }

        @Override
        public Optional<Payment> findByInvoiceId(UUID invoiceId) {
            return byId.values().stream()
                    .filter(payment -> payment.invoiceId().equals(invoiceId))
                    .findFirst();
        }

        @Override
        public List<Payment> findByInvoiceId(UUID invoiceId, int limit, int offset) {
            return byId.values().stream()
                    .filter(payment -> payment.invoiceId().equals(invoiceId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryPaymentOutboxRepository implements PaymentOutboxRepository {
        private final Map<UUID, PaymentOutboxMessage> byId = new HashMap<>();

        @Override
        public PaymentOutboxMessage save(PaymentOutboxMessage message) {
            byId.put(message.id(), message);
            return message;
        }

        @Override
        public PaymentOutboxMessage update(PaymentOutboxMessage message) {
            byId.put(message.id(), message);
            return message;
        }

        @Override
        public Optional<PaymentOutboxMessage> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<PaymentOutboxMessage> findDueForProcessing(Instant now, int limit) {
            return byId.values().stream()
                    .filter(message -> message.status() == PaymentOutboxStatus.PENDING || message.status() == PaymentOutboxStatus.FAILED)
                    .filter(message -> !message.nextAttemptAt().isAfter(now))
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryPaymentWebhookEventRepository implements PaymentWebhookEventRepository {
        private final Map<UUID, PaymentWebhookEvent> byId = new HashMap<>();
        private final List<String> locks = new ArrayList<>();

        @Override
        public void acquireProcessingLock(String providerCode, String externalEventId) {
            locks.add(providerCode + ":" + externalEventId);
        }

        @Override
        public PaymentWebhookEvent save(PaymentWebhookEvent event) {
            byId.put(event.id(), event);
            return event;
        }

        @Override
        public PaymentWebhookEvent update(PaymentWebhookEvent event) {
            byId.put(event.id(), event);
            return event;
        }

        @Override
        public Optional<PaymentWebhookEvent> findProcessedByProviderAndExternalEventId(String providerCode, String externalEventId) {
            return byId.values().stream()
                    .filter(event -> event.providerCode().equals(providerCode))
                    .filter(event -> event.externalEventId().equals(externalEventId))
                    .filter(PaymentWebhookEvent::processed)
                    .findFirst();
        }

        @Override
        public Optional<PaymentWebhookEvent> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }
    }
}
