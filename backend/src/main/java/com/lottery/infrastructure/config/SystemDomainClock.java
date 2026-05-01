package com.lottery.infrastructure.config;

import com.lottery.domain.service.DomainClock;
import java.time.Instant;

public final class SystemDomainClock implements DomainClock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
