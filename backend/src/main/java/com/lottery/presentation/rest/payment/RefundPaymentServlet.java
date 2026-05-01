package com.lottery.presentation.rest.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.RefundPaymentCommand;
import com.lottery.application.dto.PaymentDto;
import com.lottery.application.usecase.payment.RefundPaymentUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public final class RefundPaymentServlet extends JsonServlet {
    private final RefundPaymentUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public RefundPaymentServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            RefundPaymentUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RefundRequest body = readJson(request, RefundRequest.class);
            PaymentDto result = useCase.execute(
                    new RefundPaymentCommand(paymentIdFromPath(request), body.idempotencyKey()),
                    contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private UUID paymentIdFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.endsWith("/refund")) {
            throw new IllegalArgumentException("Expected path /{paymentId}/refund");
        }
        return UUID.fromString(pathInfo.substring(1, pathInfo.length() - "/refund".length()));
    }

    public record RefundRequest(String idempotencyKey) {
    }
}
