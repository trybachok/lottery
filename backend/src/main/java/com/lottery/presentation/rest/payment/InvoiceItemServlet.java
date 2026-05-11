package com.lottery.presentation.rest.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.NotFoundException;
import com.lottery.application.command.CancelInvoiceCommand;
import com.lottery.application.command.SimulateMockPaymentWebhookCommand;
import com.lottery.application.usecase.payment.CancelInvoiceUseCase;
import com.lottery.application.usecase.payment.ExpireInvoiceUseCase;
import com.lottery.application.usecase.payment.GetInvoiceUseCase;
import com.lottery.application.usecase.payment.SimulateMockPaymentWebhookUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class InvoiceItemServlet extends JsonServlet {
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final ExpireInvoiceUseCase expireInvoiceUseCase;
    private final SimulateMockPaymentWebhookUseCase simulateMockPaymentWebhookUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public InvoiceItemServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            GetInvoiceUseCase getInvoiceUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            ExpireInvoiceUseCase expireInvoiceUseCase,
            SimulateMockPaymentWebhookUseCase simulateMockPaymentWebhookUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.expireInvoiceUseCase = expireInvoiceUseCase;
        this.simulateMockPaymentWebhookUseCase = simulateMockPaymentWebhookUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if (path.action() != null) {
                throw new NotFoundException("Endpoint");
            }
            writeJson(response, 200, getInvoiceUseCase.execute(path.invoiceId(), contextFactory.from(request)));
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
            switch (path.action()) {
                case "cancel" -> {
                    IdempotencyRequest body = readJson(request, IdempotencyRequest.class);
                    writeJson(
                            response,
                            200,
                            cancelInvoiceUseCase.execute(
                                    new CancelInvoiceCommand(path.invoiceId(), body.idempotencyKey()),
                                    contextFactory.from(request)));
                }
                case "expire" -> writeJson(response, 200, expireInvoiceUseCase.execute(path.invoiceId(), contextFactory.from(request)));
                case "mock-webhook" -> {
                    MockWebhookRequest body = readJson(request, MockWebhookRequest.class);
                    writeJson(
                            response,
                            200,
                            simulateMockPaymentWebhookUseCase.execute(
                                    new SimulateMockPaymentWebhookCommand(path.invoiceId(), body.eventType()),
                                    contextFactory.from(request)));
                }
                default -> throw new NotFoundException("Endpoint");
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private PathParts path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new NotFoundException("Endpoint");
        }
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length < 1 || parts.length > 2) {
            throw new NotFoundException("Endpoint");
        }
        return new PathParts(UUID.fromString(parts[0]), parts.length == 2 ? parts[1] : null);
    }

    public record IdempotencyRequest(String idempotencyKey) {
    }

    public record MockWebhookRequest(String eventType) {
    }

    private record PathParts(UUID invoiceId, String action) {
    }
}
