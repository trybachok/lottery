package com.lottery.application.usecase.draw;

import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.DrawResultDto;
import com.lottery.application.mapper.DrawResultMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.UUID;

public final class GetDrawResultUseCase {
    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DrawResultMapper mapper;

    public GetDrawResultUseCase(
            DrawRepository drawRepository,
            DrawResultRepository drawResultRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DrawResultMapper mapper) {
        this.drawRepository = drawRepository;
        this.drawResultRepository = drawResultRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public DrawResultDto execute(UUID drawId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_RESULT_READ);
            Draw draw = drawRepository.findById(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            ensureManagerScope(draw, context);
            DrawResult result = drawResultRepository.findByDrawId(drawId).orElseThrow(() -> new NotFoundException("DrawResult"));
            return mapper.toDto(result);
        });
    }

    private void ensureManagerScope(Draw draw, UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN) || !authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            return;
        }
        if (context.actorUserId() == null || !draw.managerId().map(context.actorUserId()::equals).orElse(false)) {
            throw new ForbiddenException(PermissionCodes.DRAW_RESULT_READ);
        }
    }
}
