package com.lottery.domain.repository;

import com.lottery.domain.model.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Ticket update(Ticket ticket);

    Optional<Ticket> findById(UUID id);

    List<Ticket> findAll(int limit, int offset);

    List<Ticket> findByUserId(UUID userId, int limit, int offset);

    List<Ticket> findPaidByDrawId(UUID drawId);
}
