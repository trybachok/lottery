package com.lottery.domain.policy;

import com.lottery.domain.model.Draw;
import com.lottery.domain.valueobject.DrawStatus;

public final class TicketPurchasePolicy {
    public boolean canCreateTicketFor(Draw draw, boolean ticketIsTest) {
        if (draw.deletedAt().isPresent()) {
            return false;
        }
        if (draw.status() != DrawStatus.ACTIVE && draw.status() != DrawStatus.TEST) {
            return false;
        }
        return draw.test() || !ticketIsTest;
    }
}
