package com.lottery.domain.valueobject;

import java.util.UUID;

public final class DomainIds {
    private DomainIds() {
    }

    public static UUID newId() {
        return UUID.randomUUID();
    }
}
