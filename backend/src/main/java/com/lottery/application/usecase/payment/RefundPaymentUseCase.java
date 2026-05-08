package com.lottery.application.usecase.payment;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.command.RefundPaymentCommand;
import com.lottery.application.dto.PaymentDto;
import com.lottery.application.mapper.PaymentMapper;
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
import com.lottery.domain.valueobject.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RefundPaymentUseCase {
    private static final Logger log = LoggerFactory.getLogger(RefundPaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TicketRepository ticketRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final PaymentMapper mapper;
    private final AuditService auditService;

    public RefundPaymentUseCase(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            TicketRepository ticketRepository,
            PaymentOutboxRepository outboxRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            PaymentMapper mapper) {
        this(
                paymentRepository,
                invoiceRepository,
                ticketRepository,
                outboxRepository,
                authorizationPort,
                transactionManager,
                clock,
                mapper,
                null);
    }

    public RefundPaymentUseCase(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            TicketRepository ticketRepository,
            PaymentOutboxRepository outboxRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            PaymentMapper mapper,
            AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.ticketRepository = ticketRepository;
        this.outboxRepository = outboxRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public PaymentDto execute(RefundPaymentCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PAYMENT_REFUND);
            Payment payment = paymentRepository.findById(command.paymentId()).orElseThrow(() -> new NotFoundException("Payment"));
            if (payment.status() != PaymentStatus.CAPTURED && payment.status() != PaymentStatus.REFUNDED) {
                throw new ConflictException("PAYMENT_NOT_REFUNDABLE", "Payment is not refundable");
            }
            if (payment.status() == PaymentStatus.REFUNDED) {
                return mapper.toDto(payment);
            }
            Invoice invoice = invoiceRepository.findById(payment.invoiceId()).orElseThrow(() -> new NotFoundException("Invoice"));
            Ticket ticket = ticketRepository.findById(invoice.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
            var refundRequestedAt = clock.now();
            invoiceRepository.update(invoice.withStatus(InvoiceStatus.REFUND_PENDING, refundRequestedAt));
            ticketRepository.update(ticket.withPaymentStatus(TicketStatus.REFUND_PENDING, refundRequestedAt));
            outboxRepository.save(PaymentOutboxMessage.pending(
                    PaymentOutboxType.REFUND_PAYMENT,
                    invoice.id(),
                    payment.id(),
                    payment.providerCode(),
                    "{\"idempotencyKey\":\"" + escape(command.idempotencyKey()) + "\"}",
                    refundRequestedAt));
            if (auditService != null) {
                auditService.record(context, "PAYMENT_REFUND_REQUESTED", "PAYMENT", payment.id());
            }
            log.info("requestId={} paymentId={} invoiceId={} payment_refund_enqueued", context.requestId(), payment.id(), invoice.id());
            return mapper.toDto(payment);
        });
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
