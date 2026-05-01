package com.lottery.presentation.rest.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CreateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.query.ListDrawsQuery;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.application.usecase.draw.ListDrawsUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CreateDrawServlet extends JsonServlet {
    private final CreateDrawUseCase useCase;
    private final ListDrawsUseCase listUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public CreateDrawServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            CreateDrawUseCase useCase,
            ListDrawsUseCase listUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.listUseCase = listUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<DrawDto> result = listUseCase.execute(
                    new ListDrawsQuery(intQuery(request, "limit", 20), intQuery(request, "offset", 0)),
                    contextFactory.from(request));
            writeJson(response, 200, new DrawListResponse(result));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CreateDrawRequest body = readJson(request, CreateDrawRequest.class);
            DrawDto result = useCase.execute(
                    new CreateDrawCommand(
                            body.title(),
                            body.description(),
                            body.managerId(),
                            body.combinationSchemaId(),
                            body.salesStartAt(),
                            body.salesEndAt(),
                            body.drawAt(),
                            body.maxTickets(),
                            body.test()),
                    contextFactory.from(request));
            writeJson(response, 201, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record CreateDrawRequest(
            String title,
            String description,
            UUID managerId,
            UUID combinationSchemaId,
            Instant salesStartAt,
            Instant salesEndAt,
            Instant drawAt,
            Integer maxTickets,
            boolean test) {
    }

    public record DrawListResponse(List<DrawDto> items) {
        public DrawListResponse {
            items = List.copyOf(items);
        }
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }
}
