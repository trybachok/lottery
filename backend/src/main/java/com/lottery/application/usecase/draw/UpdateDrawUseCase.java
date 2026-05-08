package com.lottery.application.usecase.draw;

import com.lottery.application.ConflictException;
import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.command.UpdateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;

public final class UpdateDrawUseCase {
    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final DrawMapper mapper;
    private final AuditService auditService;

    public UpdateDrawUseCase(
            DrawRepository drawRepository,
            DrawResultRepository drawResultRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.drawResultRepository = drawResultRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawDto execute(UpdateDrawCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            Draw current = drawRepository.findByIdForUpdate(command.drawId()).orElseThrow(() -> new NotFoundException("Draw"));
            ensureManagerScope(current, context);
            if (current.status() == DrawStatus.COMPLETED || drawResultRepository.existsByDrawId(current.id())) {
                throw new ConflictException("DRAW_RESULT_IMMUTABLE", "Completed draw cannot be updated");
            }
            Draw updated = current.withDetails(
                    command.title() == null ? current.title() : command.title(),
                    command.description() == null ? current.description() : command.description(),
                    current.managerId().orElse(null),
                    command.salesStartAt() == null ? current.salesStartAt() : command.salesStartAt(),
                    command.salesEndAt() == null ? current.salesEndAt() : command.salesEndAt(),
                    command.drawAt() == null ? current.drawAt() : command.drawAt(),
                    command.maxTickets() == null ? current.maxTickets().orElse(null) : command.maxTickets(),
                    clock.now());
            DrawDto dto = mapper.toDto(drawRepository.update(updated));
            auditService.record(context, "DRAW_UPDATE", "DRAW", current.id());
            return dto;
        });
    }

    private void ensureManagerScope(Draw draw, UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN) || !authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            return;
        }
        if (context.actorUserId() == null || !draw.managerId().map(context.actorUserId()::equals).orElse(false)) {
            throw new ForbiddenException(PermissionCodes.DRAW_UPDATE);
        }
    }
}
