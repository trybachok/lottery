package com.lottery.application.usecase.ticket;

import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.port.transaction.TransactionManager;

public final class CreateTicketUseCase {
    private final TransactionManager transactionManager;
    private final TicketCreationService creationService;
    private final TicketMapper mapper;

    public CreateTicketUseCase(
            TransactionManager transactionManager,
            TicketCreationService creationService,
            TicketMapper mapper) {
        this.transactionManager = transactionManager;
        this.creationService = creationService;
        this.mapper = mapper;
    }

    public TicketDto execute(CreateTicketCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> mapper.toDto(creationService.create(command, context)));
    }
}
