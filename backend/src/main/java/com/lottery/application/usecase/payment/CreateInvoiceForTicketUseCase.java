package com.lottery.application.usecase.payment;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateInvoiceForTicketCommand;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.payment.PaymentProviderPort;
import com.lottery.application.port.payment.PaymentProviderPort.InvoiceCreationRequest;
import com.lottery.application.port.payment.PaymentProviderPort.InvoiceCreationResult;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Payment;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.time.Instant;

public final class CreateInvoiceForTicketUseCase {
    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderPort paymentProviderPort;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final InvoiceMapper mapper;

    public CreateInvoiceForTicketUseCase(
            TicketRepository ticketRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PaymentProviderPort paymentProviderPort,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            InvoiceMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProviderPort = paymentProviderPort;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public InvoiceDto execute(CreateInvoiceForTicketCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.TICKET_CREATE);
            Invoice existing = invoiceRepository.findByIdempotencyKey(command.idempotencyKey()).orElse(null);
            if (existing != null) {
                return mapper.toDto(existing, null);
            }
            Ticket ticket = ticketRepository.findById(command.ticketId()).orElseThrow(() -> new NotFoundException("Ticket"));
            boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                    || authorizationPort.hasRole(context, RoleCodes.MANAGER);
            if (!privileged && (context.actorUserId() == null || !context.actorUserId().equals(ticket.userId()))) {
                throw new ConflictException("TICKET_OWNERSHIP_REQUIRED", "Client can create invoice only for own ticket");
            }
            if (ticket.status() != TicketStatus.CREATED && ticket.status() != TicketStatus.PAYMENT_FAILED) {
                throw new ConflictException("TICKET_NOT_PAYABLE", "Ticket is not available for invoice creation");
            }
            InvoiceCreationResult providerResult = paymentProviderPort.createInvoice(new InvoiceCreationRequest(
                    command.providerCode(),
                    ticket.id(),
                    ticket.userId(),
                    ticket.price().amount(),
                    ticket.price().currency(),
                    command.idempotencyKey()));
            Instant now = clock.now();
            Invoice invoice = new Invoice(
                    DomainIds.newId(),
                    ticket.id(),
                    ticket.userId(),
                    providerResult.providerCode(),
                    InvoiceStatus.PENDING,
                    ticket.price(),
                    providerResult.externalInvoiceId(),
                    command.idempotencyKey(),
                    now,
                    now.plusSeconds(900),
                    null);
            Invoice saved = invoiceRepository.save(invoice);
            paymentRepository.save(new Payment(
                    DomainIds.newId(),
                    saved.id(),
                    providerResult.providerCode(),
                    PaymentStatus.INITIATED,
                    saved.amount(),
                    providerResult.externalPaymentId(),
                    now,
                    now));
            ticketRepository.update(ticket.withPaymentStatus(TicketStatus.PAYMENT_PENDING, now));
            return mapper.toDto(saved, providerResult.paymentUrl());
        });
    }
}
