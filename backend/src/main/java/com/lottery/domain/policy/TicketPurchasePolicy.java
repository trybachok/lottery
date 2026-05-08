package com.lottery.domain.policy;

import com.lottery.domain.model.Draw;
import com.lottery.domain.valueobject.DrawStatus;
import java.time.Instant;

public final class TicketPurchasePolicy {
    public boolean canCreateTicketFor(Draw draw, boolean ticketIsTest, Instant now) {
        if (draw.deletedAt().isPresent()) {
            return false;
        }
        if (draw.status() != DrawStatus.ACTIVE && draw.status() != DrawStatus.TEST) {
            return false;
        }
        if (draw.status() == DrawStatus.ACTIVE && (now.isBefore(draw.salesStartAt()) || now.isAfter(draw.salesEndAt()))) {
            return false;
        }
        return draw.test() || !ticketIsTest;
    }
}
