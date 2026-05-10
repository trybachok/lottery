package com.lottery.application.usecase.draw;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.ValidationException;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.DrawResultDto;
import com.lottery.application.mapper.DrawResultMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationValidatorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort.GeneratedWinningCombination;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenerateWinningCombinationUseCase {
    private static final Logger log = LoggerFactory.getLogger(GenerateWinningCombinationUseCase.class);

    private final DrawRepository drawRepository;
    private final CombinationSchemaRepository combinationSchemaRepository;
    private final DrawResultRepository drawResultRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final WinningCombinationGeneratorPort generator;
    private final CombinationValidatorPort validator;
    private final DrawStatusTransitionPolicy transitionPolicy;
    private final DomainClock clock;
    private final DrawResultMapper mapper;
    private final AuditService auditService;

    public GenerateWinningCombinationUseCase(
            DrawRepository drawRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            DrawResultRepository drawResultRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            WinningCombinationGeneratorPort generator,
            CombinationValidatorPort validator,
            DrawStatusTransitionPolicy transitionPolicy,
            DomainClock clock,
            DrawResultMapper mapper,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.combinationSchemaRepository = combinationSchemaRepository;
        this.drawResultRepository = drawResultRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.generator = generator;
        this.validator = validator;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public DrawResultDto execute(UUID drawId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> mapper.toDto(generate(drawId, context)));
    }

    DrawResult generate(UUID drawId, UseCaseContext context) {
        authorizationPort.ensurePermission(context, PermissionCodes.DRAW_RUN);
        Draw draw = drawRepository.findByIdForUpdate(drawId).orElseThrow(() -> new NotFoundException("Draw"));

        if (draw.status() == DrawStatus.COMPLETED || drawResultRepository.existsByDrawId(drawId)) {
            throw new ConflictException("DRAW_RESULT_ALREADY_EXISTS", "Draw winning combination already exists");
        }
        if (draw.status() != DrawStatus.SALES_CLOSED) {
            throw new ConflictException("DRAW_NOT_READY", "Winning combination can be generated only for SALES_CLOSED draw");
        }
        if (!transitionPolicy.canTransition(draw.status(), DrawStatus.DRAWING)) {
            throw new ConflictException("DRAW_STATUS_TRANSITION_FORBIDDEN", "Draw cannot transition to DRAWING");
        }

        CombinationSchema schema = combinationSchemaRepository
                .findById(draw.combinationSchemaId())
                .orElseThrow(() -> new NotFoundException("CombinationSchema"));
        Instant generatedAt = clock.now();
        log.info("requestId={} drawId={} generate_winning_combination_started", context.requestId(), drawId);

        GeneratedWinningCombination generated = generateAndValidate(draw, schema, context);
        DrawResult drawResult = new DrawResult(
                DomainIds.newId(),
                draw.id(),
                generated.combination(),
                generated.algorithmVersion(),
                generated.randomProvider(),
                generated.proofHash(),
                context.actorUserId(),
                generatedAt,
                context.requestId(),
                context.correlationId());

        DrawResult savedResult = drawResultRepository.save(drawResult);
        drawRepository.update(draw.withStatus(DrawStatus.DRAWING, generatedAt));
        if (auditService != null) {
            auditService.record(context, "DRAW_WINNING_COMBINATION_GENERATE", "DRAW", draw.id());
        }
        log.info(
                "requestId={} drawId={} resultId={} algorithmVersion={} randomProvider={} generate_winning_combination_completed",
                context.requestId(),
                draw.id(),
                savedResult.id(),
                savedResult.algorithmVersion(),
                savedResult.randomProvider());
        return savedResult;
    }

    private GeneratedWinningCombination generateAndValidate(Draw draw, CombinationSchema schema, UseCaseContext context) {
        try {
            GeneratedWinningCombination generated = generator.generate(schema);
            validator.validate(generated.combination(), schema);
            return generated;
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "requestId={} drawId={} schemaId={} generate_winning_combination_failed",
                    context.requestId(),
                    draw.id(),
                    schema.id(),
                    exception);
            throw new ValidationException("Winning combination cannot be generated for draw schema");
        }
    }
}
