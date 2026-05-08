package com.lottery.application.usecase.payment;

import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.InvoiceDto;
import java.util.UUID;

public final class ExpireInvoiceUseCase {
    private final CancelInvoiceUseCase closeUseCase;

    public ExpireInvoiceUseCase(CancelInvoiceUseCase closeUseCase) {
        this.closeUseCase = closeUseCase;
    }

    public InvoiceDto execute(UUID invoiceId, UseCaseContext context) {
        return closeUseCase.expire(invoiceId, context);
    }
}
