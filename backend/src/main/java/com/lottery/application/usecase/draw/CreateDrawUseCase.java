package com.lottery.application.usecase.draw;

import com.lottery.application.UseCaseContext;
import com.lottery.application.command.CreateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;

public final class CreateDrawUseCase {
    private final DrawRepository drawRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final DrawMapper mapper;

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
    }

    public DrawDto execute(CreateDrawCommand command, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.DRAW_CREATE);
        return transactionManager.inTransaction(() -> {
            Draw draw = Draw.create(
                    command.title(),
                    command.description(),
                    command.managerId(),
                    command.combinationSchemaId(),
                    command.salesStartAt(),
                    command.salesEndAt(),
                    command.drawAt(),
                    command.maxTickets(),
                    command.test(),
                    clock.now());
            return mapper.toDto(drawRepository.save(draw));
        });
    }
}
