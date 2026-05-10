package com.lottery.application.usecase.draw;

import com.lottery.application.ForbiddenException;
import com.lottery.application.ValidationException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.command.CreateCombinationSchemaCommand;
import com.lottery.application.command.CreateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationSchemaValidatorPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.RoleCodes;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateDrawUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateDrawUseCase.class);

    private final DrawRepository drawRepository;
    private final CombinationSchemaRepository combinationSchemaRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final CombinationSchemaValidatorPort combinationSchemaValidator;
    private final DomainClock clock;
    private final DrawMapper mapper;
    private final AuditService auditService;

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper) {
        this(drawRepository, null, authorizationPort, transactionManager, null, clock, mapper, null);
    }

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this(drawRepository, null, authorizationPort, transactionManager, null, clock, mapper, auditService);
    }

    public CreateDrawUseCase(
            DrawRepository drawRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            CombinationSchemaValidatorPort combinationSchemaValidator,
            DomainClock clock,
            DrawMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.combinationSchemaRepository = combinationSchemaRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.combinationSchemaValidator = combinationSchemaValidator;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawDto execute(CreateDrawCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_CREATE);
            ensureCreatorRole(context);
            UUID managerId = resolveManagerId(command.managerId(), context);
            Instant now = clock.now();
            UUID combinationSchemaId = resolveCombinationSchemaId(command, context, now);
            Draw draw = Draw.create(
                    command.title(),
                    command.description(),
                    managerId,
                    combinationSchemaId,
                    command.salesStartAt(),
                    command.salesEndAt(),
                    command.drawAt(),
                    command.maxTickets(),
                    command.test(),
                    now);
            DrawDto dto = mapper.toDto(drawRepository.save(draw));
            if (auditService != null) {
                auditService.record(context, "DRAW_CREATE", "DRAW", dto.id());
            }
            log.info(
                    "draw_created drawId={} managerId={} combinationSchemaId={} actorUserId={}",
                    dto.id(),
                    managerId,
                    combinationSchemaId,
                    context.actorUserId());
            return dto;
        });
    }

    private void ensureCreatorRole(UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN) || authorizationPort.hasRole(context, RoleCodes.MANAGER)) {
            return;
        }
        throw new ForbiddenException(PermissionCodes.DRAW_CREATE);
    }

    private UUID resolveManagerId(UUID requestedManagerId, UseCaseContext context) {
        if (authorizationPort.hasRole(context, RoleCodes.ADMIN)) {
            return requestedManagerId;
        }
        UUID actorUserId = context.actorUserId();
        if (actorUserId == null) {
            throw new ForbiddenException(PermissionCodes.DRAW_CREATE);
        }
        if (requestedManagerId == null) {
            return actorUserId;
        }
        if (requestedManagerId.equals(actorUserId)) {
            return requestedManagerId;
        }
        log.warn(
                "draw_create_forbidden requestedManagerId={} actorUserId={} reason=manager_scope",
                requestedManagerId,
                actorUserId);
        throw new ForbiddenException(PermissionCodes.DRAW_CREATE);
    }

    private UUID resolveCombinationSchemaId(CreateDrawCommand command, UseCaseContext context, Instant now) {
        if (command.combinationSchemaId() != null) {
            if (combinationSchemaRepository != null && combinationSchemaRepository.findById(command.combinationSchemaId()).isEmpty()) {
                throw new ValidationException("Combination schema was not found");
            }
            return command.combinationSchemaId();
        }
        if (combinationSchemaRepository == null || combinationSchemaValidator == null) {
            throw new IllegalStateException("Combination schema creation is not configured");
        }
        CombinationSchema schema = createSchema(command.combinationSchema(), now);
        try {
            combinationSchemaValidator.validateSchema(schema);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Invalid combination schema: " + exception.getMessage());
        }
        CombinationSchema saved = combinationSchemaRepository.save(schema);
        if (auditService != null) {
            auditService.record(context, "COMBINATION_SCHEMA_CREATE", "COMBINATION_SCHEMA", saved.id());
        }
        log.info(
                "combination_schema_created combinationSchemaId={} actorUserId={} name={}",
                saved.id(),
                context.actorUserId(),
                saved.name());
        return saved.id();
    }

    private CombinationSchema createSchema(CreateCombinationSchemaCommand command, Instant now) {
        Objects.requireNonNull(command, "combinationSchema");
        return new CombinationSchema(
                DomainIds.newId(),
                command.name().trim(),
                new CombinationSchemaDefinition(command.definitionJson().trim()),
                now);
    }
}
