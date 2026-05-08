package com.lottery.application.usecase.ticket;

import com.lottery.application.ValidationException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.command.BulkCreateTicketsCommand;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.dto.TicketListDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.List;

public final class BulkCreateTicketsUseCase {
    public static final int MAX_BATCH_SIZE = 100;

    private final TransactionManager transactionManager;
    private final TicketCreationService creationService;
    private final TicketMapper mapper;

    public BulkCreateTicketsUseCase(
            TransactionManager transactionManager,
            TicketCreationService creationService,
            TicketMapper mapper) {
        this.transactionManager = transactionManager;
        this.creationService = creationService;
        this.mapper = mapper;
    }

    public TicketListDto execute(BulkCreateTicketsCommand command, UseCaseContext context) {
        if (command.tickets().isEmpty()) {
            throw new ValidationException("Bulk ticket request must contain at least one ticket");
        }
        if (command.tickets().size() > MAX_BATCH_SIZE) {
            throw new ValidationException("Bulk ticket request exceeds maximum batch size");
        }
        return transactionManager.inTransaction(() -> {
            List<CreateTicketCommand> tickets = command.tickets();
            List<com.lottery.application.dto.TicketDto> result = new ArrayList<>(tickets.size());
            for (CreateTicketCommand ticket : tickets) {
                result.add(mapper.toDto(creationService.create(ticket, context)));
            }
            return new TicketListDto(result);
        });
    }
}
