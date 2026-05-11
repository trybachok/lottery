package com.lottery.application.usecase.admin;

import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.ValidationException;
import com.lottery.application.audit.AuditService;
import com.lottery.application.command.PrizeCommand;
import com.lottery.application.dto.PrizeDto;
import com.lottery.application.mapper.PrizeMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Prize;
import com.lottery.domain.repository.PrizeRepository;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.PermissionCodes;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdminPrizeUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminPrizeUseCase.class);
    private static final Set<String> PRODUCT_TYPES = Set.of("FOOD_PRODUCT", "SPORT_SUPPLEMENT");

    private final PrizeRepository prizeRepository;
    private final AuthorizationPort authorizationPort;
    private final TransactionManager transactionManager;
    private final PrizeMapper mapper;
    private final AuditService auditService;

    public AdminPrizeUseCase(
            PrizeRepository prizeRepository,
            AuthorizationPort authorizationPort,
            TransactionManager transactionManager,
            PrizeMapper mapper,
            AuditService auditService) {
        this.prizeRepository = prizeRepository;
        this.authorizationPort = authorizationPort;
        this.transactionManager = transactionManager;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<PrizeDto> listPrizes(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            return prizeRepository.findAll(limit, offset).stream().map(mapper::toDto).toList();
        });
    }

    public PrizeDto getPrize(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            return mapper.toDto(prizeRepository.findById(id).orElseThrow(() -> new NotFoundException("Prize")));
        });
    }

    public PrizeDto createPrize(PrizeCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            Prize prize = toPrize(DomainIds.newId(), command);
            PrizeDto dto = mapper.toDto(prizeRepository.save(prize));
            auditService.recordChange(context, "PRIZE_CREATE", "PRIZE", dto.id(), null, prizeSnapshot(dto));
            log.info("requestId={} actorUserId={} prizeId={} prize_created", context.requestId(), context.actorUserId(), dto.id());
            return dto;
        });
    }

    public PrizeDto updatePrize(UUID id, PrizeCommand command, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.DRAW_UPDATE);
            Prize current = prizeRepository.findById(id).orElseThrow(() -> new NotFoundException("Prize"));
            Prize updated = toPrize(id, command);
            PrizeDto dto = mapper.toDto(prizeRepository.update(updated));
            auditService.recordChange(context, "PRIZE_UPDATE", "PRIZE", id, prizeSnapshot(mapper.toDto(current)), prizeSnapshot(dto));
            log.info("requestId={} actorUserId={} prizeId={} prize_updated", context.requestId(), context.actorUserId(), id);
            return dto;
        });
    }

    private Prize toPrize(UUID id, PrizeCommand command) {
        if (command == null) {
            throw new ValidationException("Prize request is required");
        }
        String type = required(command.type(), "Prize type").toUpperCase();
        String name = required(command.name(), "Prize name");
        if ("MONEY".equals(type)) {
            BigDecimal amount = requireNonNegative(command.amount(), "Prize amount");
            Currency currency = parseCurrency(required(command.currency(), "Prize currency"));
            return new Prize(id, type, name, amount, currency, null, null, null);
        }
        if (PRODUCT_TYPES.contains(type)) {
            BigDecimal quantity = requireNonNegative(command.quantity(), "Prize quantity");
            if (command.productId() == null) {
                throw new ValidationException("Prize product id is required");
            }
            String unit = required(command.unit(), "Prize unit");
            return new Prize(id, type, name, null, null, command.productId(), quantity, unit);
        }
        throw new ValidationException("Unsupported prize type");
    }

    private BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " must not be negative");
        }
        return value;
    }

    private Currency parseCurrency(String currency) {
        try {
            return Currency.getInstance(currency.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Prize currency must be an ISO 4217 code");
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private Map<String, Object> prizeSnapshot(PrizeDto prize) {
        return Map.of(
                "id", prize.id(),
                "type", prize.type(),
                "name", prize.name());
    }
}
