package com.lottery.application.usecase.draw;

import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.application.query.ListDrawsQuery;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.List;

public final class ListDrawsUseCase {
    private final DrawRepository drawRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DrawMapper mapper;

    public ListDrawsUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DrawMapper mapper) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
    }

    public List<DrawDto> execute(ListDrawsQuery query, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_READ);
            if (!authorizationPort.hasRole(context, RoleCodes.ADMIN) && authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
                return drawRepository.findReport(null, context.actorUserId(), null, null, null, query.limit(), query.offset()).stream()
                        .map(mapper::toDto)
                        .toList();
            }
            return drawRepository.findAll(query.limit(), query.offset()).stream().map(mapper::toDto).toList();
        });
    }
}
