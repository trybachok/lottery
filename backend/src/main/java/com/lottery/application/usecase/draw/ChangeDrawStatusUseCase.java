package com.lottery.application.usecase.draw;

import com.lottery.application.ConflictException;
import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.util.UUID;

public final class ChangeDrawStatusUseCase {
    private final DrawRepository drawRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final DrawStatusTransitionPolicy transitionPolicy;
    private final DomainClock clock;
    private final DrawMapper mapper;
    private final AuditService auditService;

    public ChangeDrawStatusUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DrawStatusTransitionPolicy transitionPolicy,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawDto execute(UUID drawId, DrawLifecycleAction action, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, action.permissionCode());
            Draw current = drawRepository.findByIdForUpdate(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            ensureManagerScope(current, action, context);
            if (!transitionPolicy.canTransition(current.status(), action.targetStatus())) {
                throw new ConflictException(
                        "DRAW_STATUS_TRANSITION_FORBIDDEN",
                        "Draw cannot transition from " + current.status() + " to " + action.targetStatus());
            }
            DrawDto dto = mapper.toDto(drawRepository.update(current.withStatus(action.targetStatus(), clock.now())));
            auditService.record(context, action.auditAction(), "DRAW", current.id());
            return dto;
        });
    }

    private void ensureManagerScope(Draw draw, DrawLifecycleAction action, UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN) || !authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            return;
        }
        if (context.actorUserId() == null || !draw.managerId().map(context.actorUserId()::equals).orElse(false)) {
            throw new ForbiddenException(action.permissionCode());
        }
    }

    public enum DrawLifecycleAction {
        ACTIVATE(DrawStatus.ACTIVE, PermissionCodes.DRAW_UPDATE, "DRAW_ACTIVATE"),
        PAUSE(DrawStatus.PAUSED, PermissionCodes.DRAW_UPDATE, "DRAW_PAUSE"),
        POSTPONE(DrawStatus.POSTPONED, PermissionCodes.DRAW_UPDATE, "DRAW_POSTPONE"),
        CLOSE_SALES(DrawStatus.SALES_CLOSED, PermissionCodes.DRAW_UPDATE, "DRAW_CLOSE_SALES"),
        CANCEL(DrawStatus.CANCELLED, PermissionCodes.DRAW_CANCEL, "DRAW_CANCEL"),
        ARCHIVE(DrawStatus.ARCHIVED, PermissionCodes.DRAW_UPDATE, "DRAW_ARCHIVE");

        private final DrawStatus targetStatus;
        private final String permissionCode;
        private final String auditAction;

        DrawLifecycleAction(DrawStatus targetStatus, String permissionCode, String auditAction) {
            this.targetStatus = targetStatus;
            this.permissionCode = permissionCode;
            this.auditAction = auditAction;
        }

        public DrawStatus targetStatus() {
            return targetStatus;
        }

        public String permissionCode() {
            return permissionCode;
        }

        public String auditAction() {
            return auditAction;
        }
    }
}

