package com.lottery.application.usecase.draw;

import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.UUID;

public final class GetDrawUseCase {
    private final DrawRepository drawRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DrawMapper mapper;

    public GetDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DrawMapper mapper) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public DrawDto execute(UUID drawId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_READ);
            Draw draw = drawRepository.findById(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            ensureManagerScope(draw, context);
            return mapper.toDto(draw);
        });
    }

    private void ensureManagerScope(Draw draw, UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN) || !authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            return;
        }
        if (context.actorUserId() == null || !draw.managerId().map(context.actorUserId()::equals).orElse(false)) {
            throw new ForbiddenException(PermissionCodes.DRAW_READ);
        }
    }
}

