package com.lottery.presentation.rest.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.NotFoundException;
import com.lottery.application.command.UpdateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.dto.DrawResultDto;
import com.lottery.application.dto.RunDrawResultDto;
import com.lottery.application.usecase.draw.ChangeDrawStatusUseCase;
import com.lottery.application.usecase.draw.ChangeDrawStatusUseCase.DrawLifecycleAction;
import com.lottery.application.usecase.draw.GetDrawUseCase;
import com.lottery.application.usecase.draw.GetDrawResultUseCase;
import com.lottery.application.usecase.draw.RunDrawUseCase;
import com.lottery.application.usecase.draw.UpdateDrawUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public final class DrawItemServlet extends JsonServlet {
    private final GetDrawUseCase getDrawUseCase;
    private final UpdateDrawUseCase updateDrawUseCase;
    private final ChangeDrawStatusUseCase changeStatusUseCase;
    private final RunDrawUseCase runDrawUseCase;
    private final GetDrawResultUseCase getDrawResultUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public DrawItemServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            GetDrawUseCase getDrawUseCase,
            UpdateDrawUseCase updateDrawUseCase,
            ChangeDrawStatusUseCase changeStatusUseCase,
            RunDrawUseCase runDrawUseCase,
            GetDrawResultUseCase getDrawResultUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.getDrawUseCase = getDrawUseCase;
        this.updateDrawUseCase = updateDrawUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.runDrawUseCase = runDrawUseCase;
        this.getDrawResultUseCase = getDrawResultUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if (path.action() != null) {
                if ("result".equals(path.action())) {
                    DrawResultDto result = getDrawResultUseCase.execute(path.drawId(), contextFactory.from(request));
                    writeJson(response, 200, result);
                    return;
                }
                throw new NotFoundException("Endpoint");
            }
            DrawDto result = getDrawUseCase.execute(path.drawId(), contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if (path.action() != null) {
                throw new NotFoundException("Endpoint");
            }
            UpdateDrawRequest body = readJson(request, UpdateDrawRequest.class);
            DrawDto result = updateDrawUseCase.execute(
                    new UpdateDrawCommand(
                            path.drawId(),
                            body.title(),
                            body.description(),
                            body.salesStartAt(),
                            body.salesEndAt(),
                            body.drawAt(),
                            body.maxTickets()),
                    contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if (path.action() == null) {
                throw new NotFoundException("Endpoint");
            }
            if ("run".equals(path.action())) {
                RunDrawResultDto result = runDrawUseCase.execute(path.drawId(), contextFactory.from(request));
                writeJson(response, 200, result);
                return;
            }
            DrawLifecycleAction action = lifecycleAction(path.action());
            DrawDto result = changeStatusUseCase.execute(path.drawId(), action, contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private DrawLifecycleAction lifecycleAction(String action) {
        return switch (action) {
            case "activate" -> DrawLifecycleAction.ACTIVATE;
            case "pause" -> DrawLifecycleAction.PAUSE;
            case "postpone" -> DrawLifecycleAction.POSTPONE;
            case "close-sales" -> DrawLifecycleAction.CLOSE_SALES;
            case "cancel" -> DrawLifecycleAction.CANCEL;
            case "archive" -> DrawLifecycleAction.ARCHIVE;
            default -> throw new IllegalArgumentException("Unsupported draw action: " + action);
        };
    }

    private PathParts path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Expected path /{drawId}");
        }
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Expected path /{drawId} or /{drawId}/{action}");
        }
        return new PathParts(UUID.fromString(parts[0]), parts.length == 2 ? parts[1] : null);
    }

    public record UpdateDrawRequest(
            String title,
            String description,
            Instant salesStartAt,
            Instant salesEndAt,
            Instant drawAt,
            Integer maxTickets) {
    }

    private record PathParts(UUID drawId, String action) {
    }
}
