package com.lottery.domain.policy;

import com.lottery.domain.model.Draw;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.TicketStatus;

public final class TicketParticipationPolicy {
    public boolean canParticipate(Ticket ticket, Draw draw, boolean providerPaymentConfirmed) {
        if (ticket.status() != TicketStatus.PAID) {
            return false;
        }
        if (draw.status() != DrawStatus.SALES_CLOSED && draw.status() != DrawStatus.DRAWING) {
            return false;
        }
        if (ticket.cancelledAt().isPresent() || ticket.deletedAt().isPresent()) {
            return false;
        }
        if (!providerPaymentConfirmed) {
            return false;
        }
        return draw.test() || !ticket.test();
    }
}
