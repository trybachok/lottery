package com.lottery.domain.service;

import java.time.Instant;

public interface DomainClock {
    Instant now();
}
