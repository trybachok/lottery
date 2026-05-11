package com.lottery.presentation.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.WinningRuleCommand;
import com.lottery.application.dto.WinningRuleDto;
import com.lottery.application.ValidationException;
import com.lottery.application.usecase.admin.AdminWinningRuleUseCase;
import com.lottery.application.usecase.draw.AssignDrawManagerUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class AssignDrawManagerServlet extends JsonServlet {
    private final AssignDrawManagerUseCase assignDrawManagerUseCase;
    private final AdminWinningRuleUseCase winningRuleUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AssignDrawManagerServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler,
            AssignDrawManagerUseCase assignDrawManagerUseCase,
            AdminWinningRuleUseCase winningRuleUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.assignDrawManagerUseCase = assignDrawManagerUseCase;
        this.winningRuleUseCase = winningRuleUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length == 2 && "winning-rules".equals(parts[1])) {
                writeJson(response, 200, new WinningRuleListResponse(
                        winningRuleUseCase.listRules(UUID.fromString(parts[0]), contextFactory.from(request))));
                return;
            }
            response.sendError(404);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 2 || !"assign-manager".equals(parts[1])) {
                response.sendError(404);
                return;
            }
            AssignManagerRequest body = readJson(request, AssignManagerRequest.class);
            writeJson(response, 200, assignDrawManagerUseCase.execute(UUID.fromString(parts[0]), body.managerId(), contextFactory.from(request)));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] parts = path(request);
            if (parts.length != 2 || !"winning-rules".equals(parts[1])) {
                response.sendError(404);
                return;
            }
            WinningRuleListRequest body = readJson(request, WinningRuleListRequest.class);
            if (body.rules() == null) {
                throw new ValidationException("rules is required");
            }
            writeJson(response, 200, new WinningRuleListResponse(winningRuleUseCase.replaceRules(
                    UUID.fromString(parts[0]),
                    body.rules().stream().map(WinningRuleRequest::toCommand).toList(),
                    contextFactory.from(request))));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? new String[0] : path.substring(1).split("/");
    }

    public record AssignManagerRequest(UUID managerId) {
    }

    public record WinningRuleRequest(
            BigDecimal matchPercentFrom,
            BigDecimal matchPercentTo,
            UUID prizeId,
            int priority) {
        WinningRuleCommand toCommand() {
            return new WinningRuleCommand(matchPercentFrom, matchPercentTo, prizeId, priority);
        }
    }

    public record WinningRuleListRequest(List<WinningRuleRequest> rules) {
    }

    public record WinningRuleListResponse(List<WinningRuleDto> items) {
    }
}
