package com.lottery.application.usecase.payment;

import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.Comparator;
import java.util.UUID;

public final class GetTicketInvoiceUseCase {
    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final InvoiceMapper mapper;

    public GetTicketInvoiceUseCase(
            TicketRepository ticketRepository,
            InvoiceRepository invoiceRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            InvoiceMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.invoiceRepository = invoiceRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public InvoiceDto execute(UUID ticketId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PAYMENT_READ);
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
            Invoice invoice = invoiceRepository.findByTicketId(ticket.id()).stream()
                    .max(Comparator.comparing(Invoice::createdAt))
                    .orElseThrow(() -> new NotFoundException("Invoice"));
            InvoiceAccess.ensureCanAccess(invoice, context, authorizationPort);
            return mapper.toDto(invoice, null);
        });
    }
}
