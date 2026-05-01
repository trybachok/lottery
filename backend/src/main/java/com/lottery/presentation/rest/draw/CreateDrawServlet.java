package com.lottery.presentation.rest.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CreateDrawCommand;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public final class CreateDrawServlet extends JsonServlet {
    private final CreateDrawUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public CreateDrawServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            CreateDrawUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
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
}
