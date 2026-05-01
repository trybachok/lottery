package com.lottery.domain.policy;

import com.lottery.domain.valueobject.DrawStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class DrawStatusTransitionPolicy {
    private static final Map<DrawStatus, Set<DrawStatus>> ALLOWED = new EnumMap<>(DrawStatus.class);

    static {
        ALLOWED.put(DrawStatus.DRAFT, EnumSet.of(DrawStatus.SCHEDULED, DrawStatus.ACTIVE, DrawStatus.TEST, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.SCHEDULED, EnumSet.of(DrawStatus.ACTIVE, DrawStatus.POSTPONED, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.ACTIVE, EnumSet.of(DrawStatus.PAUSED, DrawStatus.SALES_CLOSED, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.PAUSED, EnumSet.of(DrawStatus.ACTIVE, DrawStatus.POSTPONED, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.POSTPONED, EnumSet.of(DrawStatus.SCHEDULED, DrawStatus.ACTIVE, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.SALES_CLOSED, EnumSet.of(DrawStatus.DRAWING, DrawStatus.CANCELLED));
        ALLOWED.put(DrawStatus.DRAWING, EnumSet.of(DrawStatus.COMPLETED));
        ALLOWED.put(DrawStatus.COMPLETED, EnumSet.of(DrawStatus.ARCHIVED));
        ALLOWED.put(DrawStatus.TEST, EnumSet.of(DrawStatus.COMPLETED, DrawStatus.CANCELLED, DrawStatus.ARCHIVED));
    }

    public boolean canTransition(DrawStatus from, DrawStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
