package com.lottery.presentation.rest.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.CreateInvoiceForTicketCommand;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.usecase.payment.CreateInvoiceForTicketUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class CreateInvoiceServlet extends JsonServlet {
    private final CreateInvoiceForTicketUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public CreateInvoiceServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            CreateInvoiceForTicketUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UUID ticketId = ticketIdFromPath(request);
            CreateInvoiceRequest body = readJson(request, CreateInvoiceRequest.class);
            InvoiceDto result = useCase.execute(
                    new CreateInvoiceForTicketCommand(ticketId, body.providerCode(), body.idempotencyKey()),
                    contextFactory.from(request));
            writeJson(response, 201, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private UUID ticketIdFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.endsWith("/invoice")) {
            throw new IllegalArgumentException("Expected path /{ticketId}/invoice");
        }
        return UUID.fromString(pathInfo.substring(1, pathInfo.length() - "/invoice".length()));
    }

    public record CreateInvoiceRequest(String providerCode, String idempotencyKey) {
    }
}
