package com.lottery.application.usecase.ticket;

import com.lottery.application.ForbiddenException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;

final class TicketAccess {
    private TicketAccess() {
    }

    static void ensureCanAccess(Ticket ticket, UseCaseContext context, AuthorizationPort authorizationPort, String permissionCode) {
        boolean privileged = authorizationPort.hasRole(context, RoleCodes.ADMIN)
                || authorizationPort.hasRole(context, RoleCodes.MANAGER);
        if (!privileged && (context.actorUserId() == null || !context.actorUserId().equals(ticket.userId()))) {
            throw new ForbiddenException(permissionCode == null ? PermissionCodes.TICKET_READ : permissionCode);
        }
    }
}

