package com.lottery.presentation.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.ApplicationException;
import com.lottery.application.ConflictException;
import com.lottery.application.ForbiddenException;
import com.lottery.application.NotFoundException;
import com.lottery.application.ValidationException;
import com.lottery.presentation.middleware.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        String requestId = requestId(request);
        ErrorMapping mapping = map(exception);
        if (mapping.status() >= 500) {
            log.error("requestId={} unhandled_error code={}", requestId, mapping.code(), exception);
        }
        response.setStatus(mapping.status());
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorResponse(new ErrorResponse.ErrorBody(mapping.code(), mapping.message(), mapping.details(), requestId)));
    }

    private ErrorMapping map(Exception exception) {
        if (exception instanceof ForbiddenException applicationException) {
            return new ErrorMapping(403, applicationException.code(), applicationException.getMessage(), applicationException.details());
        }
        if (exception instanceof NotFoundException applicationException) {
            return new ErrorMapping(404, applicationException.code(), applicationException.getMessage(), applicationException.details());
        }
        if (exception instanceof ConflictException applicationException) {
            return new ErrorMapping(409, applicationException.code(), applicationException.getMessage(), applicationException.details());
        }
        if (exception instanceof ValidationException applicationException) {
            return new ErrorMapping(400, applicationException.code(), applicationException.getMessage(), applicationException.details());
        }
        if (exception instanceof ApplicationException applicationException) {
            return new ErrorMapping(400, applicationException.code(), applicationException.getMessage(), applicationException.details());
        }
        if (exception instanceof IllegalArgumentException) {
            return new ErrorMapping(400, "VALIDATION_ERROR", exception.getMessage(), Map.of());
        }
        return new ErrorMapping(500, "INTERNAL_ERROR", "Internal server error", Map.of());
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContext.ATTRIBUTE_NAME);
        if (value instanceof RequestContext requestContext) {
            return requestContext.requestId();
        }
        return "req_unknown";
    }

    private record ErrorMapping(int status, String code, String message, Map<String, Object> details) {
    }
}
