package com.lottery.application.usecase.payment;

import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import java.util.UUID;

public final class GetInvoiceUseCase {
    private final InvoiceRepository invoiceRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final InvoiceMapper mapper;

    public GetInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            InvoiceMapper mapper) {
        this.invoiceRepository = invoiceRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public InvoiceDto execute(UUID invoiceId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PAYMENT_READ);
            Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new NotFoundException("Invoice"));
            InvoiceAccess.ensureCanAccess(invoice, context, authorizationPort);
            return mapper.toDto(invoice, null);
        });
    }
}
