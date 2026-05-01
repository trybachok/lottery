package com.lottery.application.usecase.draw;

import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.UUID;

public final class AssignDrawManagerUseCase {
    private final DrawRepository drawRepository;
    private final UserRepository userRepository;
    private final RbacRepository rbacRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final DrawMapper mapper;
    private final AuditService auditService;

    public AssignDrawManagerUseCase(
            DrawRepository drawRepository,
            UserRepository userRepository,
            RbacRepository rbacRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.userRepository = userRepository;
        this.rbacRepository = rbacRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawDto execute(UUID drawId, UUID managerId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            Draw draw = drawRepository.findByIdForUpdate(drawId)
                    .orElseThrow(() -> new NotFoundException("Draw"));
            userRepository.findById(managerId).orElseThrow(() -> new NotFoundException("User"));
            if (!rbacRepository.findRoleCodesByUserId(managerId).contains(RoleCodes.MANAGER)) {
                throw new ForbiddenException("User is not manager");
            }
            DrawDto dto = mapper.toDto(drawRepository.update(draw.withManager(managerId, clock.now())));
            auditService.record(context, "ADMIN_DRAW_MANAGER_ASSIGN", "DRAW", drawId);
            return dto;
        });
    }
}
