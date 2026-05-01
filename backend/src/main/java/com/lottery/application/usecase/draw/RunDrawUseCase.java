package com.lottery.application.usecase.draw;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.dto.RunDrawResultDto;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationEvaluatorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort.GeneratedWinningCombination;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.model.WinningRule;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RunDrawUseCase {
    private final DrawRepository drawRepository;
    private final CombinationSchemaRepository combinationSchemaRepository;
    private final DrawResultRepository drawResultRepository;
    private final TicketRepository ticketRepository;
    private final WinningRuleRepository winningRuleRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final WinningCombinationGeneratorPort generator;
    private final CombinationEvaluatorPort evaluator;
    private final DrawStatusTransitionPolicy transitionPolicy;
    private final DomainClock clock;

    public RunDrawUseCase(
            DrawRepository drawRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            DrawResultRepository drawResultRepository,
            TicketRepository ticketRepository,
            WinningRuleRepository winningRuleRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            WinningCombinationGeneratorPort generator,
            CombinationEvaluatorPort evaluator,
            DrawStatusTransitionPolicy transitionPolicy,
            DomainClock clock) {
        this.drawRepository = drawRepository;
        this.combinationSchemaRepository = combinationSchemaRepository;
        this.drawResultRepository = drawResultRepository;
        this.ticketRepository = ticketRepository;
        this.winningRuleRepository = winningRuleRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.generator = generator;
        this.evaluator = evaluator;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
    }

    public RunDrawResultDto execute(UUID drawId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_RUN);
            Draw draw = drawRepository.findByIdForUpdate(drawId).orElseThrow(() -> new NotFoundException("Draw"));
            if (drawResultRepository.existsByDrawId(drawId) || draw.status() == DrawStatus.COMPLETED) {
                throw new ConflictException("DRAW_ALREADY_RUN", "Draw result already exists");
            }
            if (draw.status() != DrawStatus.SALES_CLOSED) {
                throw new ConflictException("DRAW_NOT_READY", "Draw must be in SALES_CLOSED status");
            }
            if (!transitionPolicy.canTransition(draw.status(), DrawStatus.DRAWING)) {
                throw new ConflictException("DRAW_STATUS_TRANSITION_FORBIDDEN", "Draw cannot transition to DRAWING");
            }

            Instant startedAt = clock.now();
            Draw drawing = drawRepository.update(draw.withStatus(DrawStatus.DRAWING, startedAt));
            CombinationSchema schema = combinationSchemaRepository
                    .findById(drawing.combinationSchemaId())
                    .orElseThrow(() -> new NotFoundException("CombinationSchema"));
            GeneratedWinningCombination generated = generator.generate(schema);
            DrawResult drawResult = new DrawResult(
                    DomainIds.newId(),
                    drawing.id(),
                    generated.combination(),
                    generated.algorithmVersion(),
                    generated.randomProvider(),
                    generated.proofHash(),
                    context.actorUserId(),
                    startedAt);
            DrawResult savedResult = drawResultRepository.save(drawResult);

            List<WinningRule> winningRules = winningRuleRepository.findByDrawIdOrderByPriority(drawing.id());
            List<Ticket> paidTickets = ticketRepository.findPaidByDrawId(drawing.id());
            int winningTickets = 0;
            int losingTickets = 0;
            Instant checkedAt = clock.now();
            for (Ticket ticket : paidTickets) {
                BigDecimal matchPercent = evaluator.matchPercent(ticket.combination(), generated.combination(), schema);
                WinningRule matchingRule = findMatchingRule(winningRules, matchPercent);
                if (matchingRule == null) {
                    ticketRepository.update(ticket.withDrawResult(TicketStatus.LOSE, matchPercent, null, checkedAt));
                    losingTickets++;
                } else {
                    ticketRepository.update(ticket.withDrawResult(TicketStatus.WIN, matchPercent, matchingRule.prizeId(), checkedAt));
                    winningTickets++;
                }
            }

            if (!transitionPolicy.canTransition(DrawStatus.DRAWING, DrawStatus.COMPLETED)) {
                throw new ConflictException("DRAW_STATUS_TRANSITION_FORBIDDEN", "Draw cannot transition to COMPLETED");
            }
            Instant completedAt = clock.now();
            drawRepository.update(drawing.withStatus(DrawStatus.COMPLETED, completedAt));
            return new RunDrawResultDto(
                    drawing.id(),
                    savedResult.id(),
                    generated.combination().values(),
                    paidTickets.size(),
                    winningTickets,
                    losingTickets,
                    completedAt);
        });
    }

    private WinningRule findMatchingRule(List<WinningRule> winningRules, BigDecimal matchPercent) {
        for (WinningRule rule : winningRules) {
            if (matchPercent.compareTo(rule.matchPercentFrom()) >= 0 && matchPercent.compareTo(rule.matchPercentTo()) <= 0) {
                return rule;
            }
        }
        return null;
    }
}
