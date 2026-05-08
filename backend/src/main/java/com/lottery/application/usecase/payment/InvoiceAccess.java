package com.lottery.application.usecase.payment;

import com.lottery.application.ConflictException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.valueobject.RoleCodes;

final class InvoiceAccess {
    private InvoiceAccess() {
    }

    static void ensureCanAccess(Invoice invoice, UseCaseContext context, AuthorizationPort authorizationPort) {
        boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                || authorizationPort.hasRole(context, RoleCodes.MANAGER);
        if (!privileged && (context.actorUserId() == null || !context.actorUserId().equals(invoice.userId()))) {
            throw new ConflictException("INVOICE_OWNERSHIP_REQUIRED", "Client can access only own invoices");
        }
    }
}
