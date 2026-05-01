package com.lottery.presentation.rest.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.command.ProcessPaymentWebhookCommand;
import com.lottery.application.dto.PaymentWebhookResultDto;
import com.lottery.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PaymentWebhookServlet extends JsonServlet {
    private final ProcessPaymentWebhookUseCase useCase;

    public PaymentWebhookServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            ProcessPaymentWebhookUseCase useCase) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PaymentWebhookResultDto result = useCase.execute(new ProcessPaymentWebhookCommand(
                    providerCodeFromPath(request),
                    payload,
                    request.getHeader("X-Mock-Signature")));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private String providerCodeFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.endsWith("/webhook")) {
            throw new IllegalArgumentException("Expected path /{providerCode}/webhook");
        }
        return pathInfo.substring(1, pathInfo.length() - "/webhook".length());
    }
}
