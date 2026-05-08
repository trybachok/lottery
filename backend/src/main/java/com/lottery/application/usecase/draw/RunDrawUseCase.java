package com.lottery.application.usecase.draw;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.RunDrawResultDto;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.lottery.CombinationEvaluatorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort.GeneratedWinningCombination;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.model.Draw;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.model.Invoice;
import com.lottery.domain.model.Payment;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.model.WinningRule;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.policy.TicketParticipationPolicy;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.PaymentStatus;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunDrawUseCase {
    private static final Logger log = LoggerFactory.getLogger(RunDrawUseCase.class);

    private final DrawRepository drawRepository;
    private final CombinationSchemaRepository combinationSchemaRepository;
    private final DrawResultRepository drawResultRepository;
    private final TicketRepository ticketRepository;
    private final WinningRuleRepository winningRuleRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final WinningCombinationGeneratorPort generator;
    private final CombinationEvaluatorPort evaluator;
    private final DrawStatusTransitionPolicy transitionPolicy;
    private final TicketParticipationPolicy ticketParticipationPolicy;
    private final DomainClock clock;
    private final AuditService auditService;

    public RunDrawUseCase(
            DrawRepository drawRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            DrawResultRepository drawResultRepository,
            TicketRepository ticketRepository,
            WinningRuleRepository winningRuleRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            WinningCombinationGeneratorPort generator,
            CombinationEvaluatorPort evaluator,
            DrawStatusTransitionPolicy transitionPolicy,
            TicketParticipationPolicy ticketParticipationPolicy,
            DomainClock clock) {
        this(
                drawRepository,
                combinationSchemaRepository,
                drawResultRepository,
                ticketRepository,
                winningRuleRepository,
                invoiceRepository,
                paymentRepository,
                authorizationPort,
                transactionManager,
                generator,
                evaluator,
                transitionPolicy,
                ticketParticipationPolicy,
                clock,
                null);
    }

    public RunDrawUseCase(
            DrawRepository drawRepository,
            CombinationSchemaRepository combinationSchemaRepository,
            DrawResultRepository drawResultRepository,
            TicketRepository ticketRepository,
            WinningRuleRepository winningRuleRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            WinningCombinationGeneratorPort generator,
            CombinationEvaluatorPort evaluator,
            DrawStatusTransitionPolicy transitionPolicy,
            TicketParticipationPolicy ticketParticipationPolicy,
            DomainClock clock,
            AuditService auditService) {
        this.drawRepository = drawRepository;
        this.combinationSchemaRepository = combinationSchemaRepository;
        this.drawResultRepository = drawResultRepository;
        this.ticketRepository = ticketRepository;
        this.winningRuleRepository = winningRuleRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.generator = generator;
        this.evaluator = evaluator;
        this.transitionPolicy = transitionPolicy;
        this.ticketParticipationPolicy = ticketParticipationPolicy;
        this.clock = clock;
        this.auditService = auditService;
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
            List<WinningRule> winningRules = winningRuleRepository.findByDrawIdOrderByPriority(draw.id());
            if (winningRules.isEmpty()) {
                throw new ConflictException("DRAW_WINNING_RULES_REQUIRED", "Draw cannot be run without winning rules");
            }

            Instant startedAt = clock.now();
            log.info("requestId={} drawId={} run_draw_started", context.requestId(), drawId);
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
                    startedAt,
                    context.requestId(),
                    context.correlationId());
            DrawResult savedResult = drawResultRepository.save(drawResult);

            List<Ticket> paidTickets = ticketRepository.findPaidByDrawId(drawing.id());
            int winningTickets = 0;
            int losingTickets = 0;
            int notParticipatingTickets = 0;
            Instant participatedAt = clock.now();
            Instant checkedAt = participatedAt;
            for (Ticket ticket : paidTickets) {
                boolean providerPaymentConfirmed = providerPaymentConfirmed(ticket);
                if (!ticketParticipationPolicy.canParticipate(ticket, drawing, providerPaymentConfirmed)) {
                    ticketRepository.update(ticket.withNotParticipated(checkedAt));
                    notParticipatingTickets++;
                    log.warn(
                            "requestId={} drawId={} ticketId={} providerPaymentConfirmed={} ticket_not_participating",
                            context.requestId(),
                            drawing.id(),
                            ticket.id(),
                            providerPaymentConfirmed);
                    continue;
                }
                BigDecimal matchPercent = evaluator.matchPercent(ticket.combination(), generated.combination(), schema);
                WinningRule matchingRule = findMatchingRule(winningRules, matchPercent);
                if (matchingRule == null) {
                    ticketRepository.update(ticket.withDrawResult(TicketStatus.LOSE, matchPercent, null, participatedAt, checkedAt));
                    losingTickets++;
                } else {
                    ticketRepository.update(
                            ticket.withDrawResult(TicketStatus.WIN, matchPercent, matchingRule.prizeId(), participatedAt, checkedAt));
                    winningTickets++;
                }
            }

            if (!transitionPolicy.canTransition(DrawStatus.DRAWING, DrawStatus.COMPLETED)) {
                throw new ConflictException("DRAW_STATUS_TRANSITION_FORBIDDEN", "Draw cannot transition to COMPLETED");
            }
            Instant completedAt = clock.now();
            drawRepository.update(drawing.withStatus(DrawStatus.COMPLETED, completedAt));
            if (auditService != null) {
                auditService.record(context, "DRAW_RUN", "DRAW", drawing.id());
            }
            int processedTickets = winningTickets + losingTickets;
            log.info(
                    "requestId={} drawId={} resultId={} processedTickets={} skippedTickets={} run_draw_completed",
                    context.requestId(),
                    drawing.id(),
                    savedResult.id(),
                    processedTickets,
                    notParticipatingTickets);
            return new RunDrawResultDto(
                    drawing.id(),
                    savedResult.id(),
                    generated.combination().values(),
                    processedTickets,
                    winningTickets,
                    losingTickets,
                    completedAt);
        });
    }

    private boolean providerPaymentConfirmed(Ticket ticket) {
        for (Invoice invoice : invoiceRepository.findByTicketId(ticket.id())) {
            if (invoice.status() != InvoiceStatus.PAID) {
                continue;
            }
            Payment payment = paymentRepository.findByInvoiceId(invoice.id()).orElse(null);
            if (payment != null && payment.status() == PaymentStatus.CAPTURED) {
                return true;
            }
        }
        return false;
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
