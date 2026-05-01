package com.lottery.presentation.rest.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.AuditLogDto;
import com.lottery.application.query.ListAuditLogsQuery;
import com.lottery.application.usecase.audit.ListAuditLogsUseCase;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditLogsServlet extends JsonServlet {
    private final ListAuditLogsUseCase useCase;
    private final ServletUseCaseContextFactory contextFactory;

    public AuditLogsServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            ListAuditLogsUseCase useCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.useCase = useCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<AuditLogDto> items = useCase.execute(query(request), contextFactory.from(request));
            writeJson(response, 200, new AuditLogListResponse(items));
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private ListAuditLogsQuery query(HttpServletRequest request) {
        return new ListAuditLogsQuery(
                uuidQuery(request, "userId"),
                request.getParameter("action"),
                request.getParameter("entityType"),
                uuidQuery(request, "entityId"),
                instantQuery(request, "dateFrom"),
                instantQuery(request, "dateTo"),
                intQuery(request, "limit", 100),
                intQuery(request, "offset", 0));
    }

    private UUID uuidQuery(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private Instant instantQuery(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public record AuditLogListResponse(List<AuditLogDto> items) {
        public AuditLogListResponse {
            items = List.copyOf(items);
        }
    }
}
