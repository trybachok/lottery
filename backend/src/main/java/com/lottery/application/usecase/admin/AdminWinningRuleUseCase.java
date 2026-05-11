package com.lottery.application.usecase.admin;

import com.lottery.application.ConflictException;
import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.ValidationException;
import com.lottery.application.audit.AuditService;
import com.lottery.application.command.WinningRuleCommand;
import com.lottery.application.dto.WinningRuleDto;
import com.lottery.application.mapper.WinningRuleMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.WinningRule;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.PrizeRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdminWinningRuleUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminWinningRuleUseCase.class);
    private static final Set<DrawStatus> EDITABLE_DRAW_STATUSES = Set.of(
            DrawStatus.DRAFT,
            DrawStatus.SCHEDULED,
            DrawStatus.ACTIVE,
            DrawStatus.PAUSED,
            DrawStatus.POSTPONED);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal STEP = new BigDecimal("5");

    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;
    private final PrizeRepository prizeRepository;
    private final WinningRuleRepository winningRuleRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final WinningRuleMapper mapper;
    private final AuditService auditService;

    public AdminWinningRuleUseCase(
            DrawRepository drawRepository,
            DrawResultRepository drawResultRepository,
            PrizeRepository prizeRepository,
            WinningRuleRepository winningRuleRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            WinningRuleMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.drawResultRepository = drawResultRepository;
        this.prizeRepository = prizeRepository;
        this.winningRuleRepository = winningRuleRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<WinningRuleDto> listRules(UUID drawId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_READ);
            Draw draw = drawRepository.findById(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            ensureDrawManagerScope(draw, context, PermissionCodes.DRAW_READ);
            return winningRuleRepository.findByDrawIdOrderByPriority(drawId).stream().map(mapper::toDto).toList();
        });
    }

    public List<WinningRuleDto> replaceRules(UUID drawId, List<WinningRuleCommand> commands, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            Draw draw = drawRepository.findByIdForUpdate(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            ensureDrawManagerScope(draw, context, PermissionCodes.DRAW_UPDATE);
            ensureEditable(draw);
            List<WinningRule> before = winningRuleRepository.findByDrawIdOrderByPriority(drawId);
            List<WinningRule> rules = toRules(drawId, commands);
            winningRuleRepository.deleteByDrawId(drawId);
            List<WinningRule> saved = rules.stream().map(winningRuleRepository::save).toList();
            auditService.recordChange(
                    context,
                    "WINNING_RULES_REPLACE",
                    "DRAW",
                    drawId,
                    Map.of("rules", before.stream().map(this::ruleSnapshot).toList()),
                    Map.of("rules", saved.stream().map(this::ruleSnapshot).toList()));
            log.info(
                    "requestId={} actorUserId={} drawId={} rulesCount={} winning_rules_replaced",
                    context.requestId(),
                    context.actorUserId(),
                    drawId,
                    saved.size());
            return saved.stream().map(mapper::toDto).toList();
        });
    }

    private List<WinningRule> toRules(UUID drawId, List<WinningRuleCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new ValidationException("At least one winning rule is required");
        }
        return commands.stream()
                .map(command -> {
                    if (command.prizeId() == null) {
                        throw new ValidationException("Prize id is required");
                    }
                    if (prizeRepository.findById(command.prizeId()).isEmpty()) {
                        throw new ValidationException("Prize was not found");
                    }
                    validatePercent(command.matchPercentFrom(), "matchPercentFrom");
                    validatePercent(command.matchPercentTo(), "matchPercentTo");
                    if (command.matchPercentFrom().compareTo(command.matchPercentTo()) > 0) {
                        throw new ValidationException("matchPercentFrom must not be greater than matchPercentTo");
                    }
                    if (command.priority() < 0) {
                        throw new ValidationException("Winning rule priority must not be negative");
                    }
                    return new WinningRule(
                            DomainIds.newId(),
                            drawId,
                            command.matchPercentFrom(),
                            command.matchPercentTo(),
                            command.prizeId(),
                            command.priority());
                })
                .toList();
    }

    private void validatePercent(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required");
        }
        if (value.compareTo(ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
            throw new ValidationException(fieldName + " must be between 0 and 100");
        }
        if (value.remainder(STEP).compareTo(ZERO) != 0) {
            throw new ValidationException(fieldName + " must use 5 percent step");
        }
    }

    private void ensureEditable(Draw draw) {
        if (drawResultRepository.existsByDrawId(draw.id())) {
            throw new ConflictException("DRAW_RESULT_ALREADY_EXISTS", "Winning rules cannot be changed after result generation");
        }
        if (!EDITABLE_DRAW_STATUSES.contains(draw.status())) {
            throw new ConflictException("DRAW_NOT_EDITABLE", "Winning rules cannot be changed for draw status " + draw.status());
        }
    }

    private void ensureDrawManagerScope(Draw draw, UseCaseContext context, String permissionCode) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN)) {
            return;
        }
        if (!authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            throw new ForbiddenException(permissionCode);
        }
        if (context.actorUserId() == null || !draw.managerId().map(context.actorUserId()::equals).orElse(false)) {
            throw new ForbiddenException(permissionCode);
        }
    }

    private Map<String, Object> ruleSnapshot(WinningRule rule) {
        return Map.of(
                "id", rule.id(),
                "drawId", rule.drawId(),
                "matchPercentFrom", rule.matchPercentFrom(),
                "matchPercentTo", rule.matchPercentTo(),
                "prizeId", rule.prizeId(),
                "priority", rule.priority());
    }
}
