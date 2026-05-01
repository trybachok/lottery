package com.lottery.application.usecase.draw;

import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
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
    private final AuditService auditService;

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper) {
        this(drawRepository, authorizationPort, transactionManager, clock, mapper, null);
    }

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawDto execute(CreateDrawCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_CREATE);
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
            DrawDto dto = mapper.toDto(drawRepository.save(draw));
            if (auditService != null) {
                auditService.record(context, "DRAW_CREATE", "DRAW", dto.id());
            }
            return dto;
        });
    }
}
