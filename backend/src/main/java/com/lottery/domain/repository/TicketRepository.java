package com.lottery.domain.repository;

import com.lottery.domain.model.Ticket;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(UUID id);
}
