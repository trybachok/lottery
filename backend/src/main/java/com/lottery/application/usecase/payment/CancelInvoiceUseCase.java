package com.lottery.application.usecase.payment;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CancelInvoiceCommand;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.port.auth.AuthorizationPort;
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
import com.lottery.domain.valueobject.PaymentOutboxType;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CancelInvoiceUseCase {
    private static final Logger log = LoggerFactory.getLogger(CancelInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final InvoiceMapper mapper;

    public CancelInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            TicketRepository ticketRepository,
            PaymentOutboxRepository outboxRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            InvoiceMapper mapper) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.outboxRepository = outboxRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public InvoiceDto execute(CancelInvoiceCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> cancel(command.invoiceId(), command.idempotencyKey(), context, false));
    }

    InvoiceDto expire(UUID invoiceId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> cancel(invoiceId, "expire-" + invoiceId, context, true));
    }

    private InvoiceDto cancel(UUID invoiceId, String idempotencyKey, UseCaseContext context, boolean expire) {
        authorizationPort.ensurePermission(context, PermissionCodes.PAYMENT_READ);
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new NotFoundException("Invoice"));
        InvoiceAccess.ensureCanAccess(invoice, context, authorizationPort);
        if (invoice.status() == InvoiceStatus.CANCELLED || invoice.status() == InvoiceStatus.EXPIRED) {
            return mapper.toDto(invoice, null);
        }
        if (invoice.status() != InvoiceStatus.CREATED && invoice.status() != InvoiceStatus.PENDING) {
            throw new ConflictException("INVOICE_NOT_CLOSABLE", "Only CREATED or PENDING invoices can be cancelled or expired");
        }
        Instant now = clock.now();
        if (expire && invoice.maybeExpiresAt().map(expiresAt -> expiresAt.isAfter(now)).orElse(true)) {
            throw new ConflictException("INVOICE_NOT_EXPIRED", "Invoice expiration time has not passed yet");
        }
        Payment payment = paymentRepository.findByInvoiceId(invoice.id()).orElse(null);
        InvoiceStatus newStatus = expire ? InvoiceStatus.EXPIRED : InvoiceStatus.CANCELLED;
        Invoice closed = invoiceRepository.update(invoice.withStatus(newStatus, now));
        if (payment != null) {
            paymentRepository.update(payment.withStatus(PaymentStatus.CANCELLED, now));
            payment.maybeExternalPaymentId().ifPresent(externalPaymentId -> outboxRepository.save(PaymentOutboxMessage.pending(
                    PaymentOutboxType.CANCEL_PAYMENT,
                    invoice.id(),
                    payment.id(),
                    payment.providerCode(),
                    "{\"idempotencyKey\":\"" + escape(idempotencyKey) + "\"}",
                    now)));
        }
        Ticket ticket = ticketRepository.findById(invoice.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
        ticketRepository.update(ticket.withPaymentReleased(now));
        log.info(
                "requestId={} invoiceId={} status={} invoice_closed",
                context.requestId(),
                invoice.id(),
                newStatus);
        return mapper.toDto(closed, null);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
