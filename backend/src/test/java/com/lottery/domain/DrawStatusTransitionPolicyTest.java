package com.lottery.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.valueobject.DrawStatus;
import org.junit.jupiter.api.Test;

final class DrawStatusTransitionPolicyTest {
    private final DrawStatusTransitionPolicy policy = new DrawStatusTransitionPolicy();

    @Test
    void allowsConfiguredTransitionsFromDraft() {
        assertTrue(policy.canTransition(DrawStatus.DRAFT, DrawStatus.SCHEDULED));
        assertTrue(policy.canTransition(DrawStatus.DRAFT, DrawStatus.ACTIVE));
        assertTrue(policy.canTransition(DrawStatus.DRAFT, DrawStatus.TEST));
        assertTrue(policy.canTransition(DrawStatus.DRAFT, DrawStatus.CANCELLED));
    }

    @Test
    void rejectsChangingCompletedResultBackToDrawing() {
        assertFalse(policy.canTransition(DrawStatus.COMPLETED, DrawStatus.DRAWING));
    }
}
