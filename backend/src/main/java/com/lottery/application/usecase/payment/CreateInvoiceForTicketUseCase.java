package com.lottery.application.usecase.payment;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateInvoiceForTicketCommand;
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
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PaymentOutboxType;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateInvoiceForTicketUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateInvoiceForTicketUseCase.class);

    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final InvoiceMapper mapper;

    public CreateInvoiceForTicketUseCase(
            TicketRepository ticketRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PaymentOutboxRepository outboxRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            InvoiceMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public InvoiceDto execute(CreateInvoiceForTicketCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CREATE);
            Ticket ticket = ticketRepository.findById(command.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
            boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                    || authorizationPort.hasRole(context, RoleCodes.MANAGER);
            if (!privileged && (context.actorUserId() == null || !context.actorUserId().equals(ticket.userId()))) {
                throw new ConflictException("TICKET_OWNERSHIP_REQUIRED", "Client can create invoice only for own ticket");
            }
            Invoice existingByIdempotency = invoiceRepository.findByIdempotencyKey(command.idempotencyKey()).orElse(null);
            if (existingByIdempotency != null) {
                if (!existingByIdempotency.ticketId().equals(ticket.id())) {
                    throw new ConflictException("INVOICE_IDEMPOTENCY_KEY_REUSED", "Idempotency key is already used for another ticket");
                }
                return mapper.toDto(existingByIdempotency, null);
            }
            Invoice activeInvoice = invoiceRepository.findActiveByTicketId(ticket.id()).orElse(null);
            if (activeInvoice != null) {
                throw new ConflictException("ACTIVE_INVOICE_EXISTS", "Ticket already has an active invoice");
            }
            if (ticket.status() != TicketStatus.CREATED && ticket.status() != TicketStatus.PAYMENT_FAILED) {
                throw new ConflictException("TICKET_NOT_PAYABLE", "Ticket is not available for invoice creation");
            }
            Instant now = clock.now();
            Invoice invoice = new Invoice(
                    DomainIds.newId(),
                    ticket.id(),
                    ticket.userId(),
                    command.providerCode(),
                    InvoiceStatus.CREATED,
                    ticket.price(),
                    null,
                    null,
                    command.idempotencyKey(),
                    now,
                    now.plusSeconds(900),
                    null);
            Invoice saved = invoiceRepository.save(invoice);
            Payment payment = paymentRepository.save(new Payment(
                    DomainIds.newId(),
                    saved.id(),
                    command.providerCode(),
                    PaymentStatus.INITIATED,
                    saved.amount(),
                    null,
                    now,
                    now));
            outboxRepository.save(PaymentOutboxMessage.pending(
                    PaymentOutboxType.CREATE_INVOICE,
                    saved.id(),
                    payment.id(),
                    command.providerCode(),
                    "{\"idempotencyKey\":\"" + escape(command.idempotencyKey()) + "\"}",
                    now));
            ticketRepository.update(ticket.withPaymentStatus(TicketStatus.PAYMENT_PENDING, now));
            log.info("requestId={} invoiceId={} ticketId={} payment_create_invoice_enqueued", context.requestId(), saved.id(), ticket.id());
            return mapper.toDto(saved, null);
        });
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
