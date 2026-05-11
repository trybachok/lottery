package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.PrizeCommand;
import com.lottery.application.dto.PrizeDto;
import com.lottery.application.usecase.admin.AdminPrizeUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class AdminPrizesServlet extends JsonServlet {
    private final AdminPrizeUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AdminPrizesServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            AdminPrizeUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length == 0) {
                writeJson(response, 200, new PrizeListResponse(useCase.listPrizes(
                        intQuery(request, "limit", 100), intQuery(request, "offset", 0), contextFactory.from(request))));
            } else if (parts.length == 1) {
                writeJson(response, 200, useCase.getPrize(UUID.fromString(parts[0]), contextFactory.from(request)));
            } else {
                response.sendError(404);
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PrizeRequest body = readJson(request, PrizeRequest.class);
            writeJson(response, 201, useCase.createPrize(body.toCommand(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 1) {
                response.sendError(404);
                return;
            }
            PrizeRequest body = readJson(request, PrizeRequest.class);
            writeJson(response, 200, useCase.updatePrize(UUID.fromString(parts[0]), body.toCommand(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? new String[0] : path.substring(1).split("/");
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public record PrizeRequest(
            String type,
            String name,
            BigDecimal amount,
            String currency,
            UUID productId,
            BigDecimal quantity,
            String unit) {
        PrizeCommand toCommand() {
            return new PrizeCommand(type, name, amount, currency, productId, quantity, unit);
        }
    }

    public record PrizeListResponse(List<PrizeDto> items) {
    }
}
