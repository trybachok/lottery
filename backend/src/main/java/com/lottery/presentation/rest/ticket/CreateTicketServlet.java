package com.lottery.presentation.rest.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.usecase.ticket.CreateTicketUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class CreateTicketServlet extends JsonServlet {
    private final CreateTicketUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public CreateTicketServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            CreateTicketUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CreateTicketRequest body = readJson(request, CreateTicketRequest.class);
            TicketDto result = useCase.execute(
                    new CreateTicketCommand(
                            body.userId(),
                            body.drawId(),
                            body.combinationValues(),
                            body.priceAmount(),
                            Currency.getInstance(body.priceCurrency()),
                            body.test()),
                    contextFactory.from(request));
            writeJson(response, 201, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    public record CreateTicketRequest(
            UUID userId,
            UUID drawId,
            List<String> combinationValues,
            BigDecimal priceAmount,
            String priceCurrency,
            boolean test) {
    }
}
