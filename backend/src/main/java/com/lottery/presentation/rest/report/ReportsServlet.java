package com.lottery.presentation.rest.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.dto.DrawDto;
import com.lottery.application.dto.ReportPageDto;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.query.DrawReportQuery;
import com.lottery.application.query.TicketReportQuery;
import com.lottery.application.usecase.report.GenerateDrawReportUseCase;
import com.lottery.application.usecase.report.GenerateTicketReportUseCase;
import com.lottery.domain.valueobject.DrawStatus;
import com.lottery.domain.valueobject.TicketStatus;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReportsServlet extends JsonServlet {
    private final GenerateDrawReportUseCase drawReportUseCase;
    private final GenerateTicketReportUseCase ticketReportUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public ReportsServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            GenerateDrawReportUseCase drawReportUseCase,
            GenerateTicketReportUseCase ticketReportUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.drawReportUseCase = drawReportUseCase;
        this.ticketReportUseCase = ticketReportUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = request.getPathInfo();
            if ("/draws".equals(path) || "/draws/export".equals(path)) {
                ReportPageDto<DrawDto> page = drawReportUseCase.execute(drawQuery(request), contextFactory.from(request));
                writeDrawResponse(request, response, page, "/draws/export".equals(path));
                return;
            }
            if ("/tickets".equals(path) || "/tickets/export".equals(path)) {
                ReportPageDto<TicketDto> page = ticketReportUseCase.execute(ticketQuery(request), contextFactory.from(request));
                writeTicketResponse(request, response, page, "/tickets/export".equals(path));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private DrawReportQuery drawQuery(HttpServletRequest request) {
        return new DrawReportQuery(
                uuidQuery(request, "drawId"),
                uuidQuery(request, "userId"),
                drawStatusQuery(request),
                instantQuery(request, "dateFrom"),
                instantQuery(request, "dateTo"),
                intQuery(request, "limit", 100),
                intQuery(request, "offset", 0));
    }

    private TicketReportQuery ticketQuery(HttpServletRequest request) {
        return new TicketReportQuery(
                uuidQuery(request, "userId"),
                uuidQuery(request, "drawId"),
                ticketStatusQuery(request),
                instantQuery(request, "dateFrom"),
                instantQuery(request, "dateTo"),
                intQuery(request, "limit", 100),
                intQuery(request, "offset", 0));
    }

    private void writeDrawResponse(HttpServletRequest request, HttpServletResponse response, ReportPageDto<DrawDto> page, boolean export)
            throws IOException {
        if (export && "csv".equalsIgnoreCase(request.getParameter("format"))) {
            response.setStatus(200);
            response.setContentType("text/csv; charset=utf-8");
            if (export) {
                response.setHeader("Content-Disposition", "attachment; filename=\"draw-report.csv\"");
            }
            response.getWriter().write(drawCsv(page.items()));
            return;
        }
        if (export) {
            response.setHeader("Content-Disposition", "attachment; filename=\"draw-report.json\"");
        }
        writeJson(response, 200, new DrawReportResponse(page.items(), page.total(), page.limit(), page.offset(), page.hasMore()));
    }

    private void writeTicketResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            ReportPageDto<TicketDto> page,
            boolean export)
            throws IOException {
        if (export && "csv".equalsIgnoreCase(request.getParameter("format"))) {
            response.setStatus(200);
            response.setContentType("text/csv; charset=utf-8");
            if (export) {
                response.setHeader("Content-Disposition", "attachment; filename=\"ticket-report.csv\"");
            }
            response.getWriter().write(ticketCsv(page.items()));
            return;
        }
        if (export) {
            response.setHeader("Content-Disposition", "attachment; filename=\"ticket-report.json\"");
        }
        writeJson(response, 200, new TicketReportResponse(page.items(), page.total(), page.limit(), page.offset(), page.hasMore()));
    }

    private String drawCsv(List<DrawDto> items) {
        StringBuilder csv = new StringBuilder("id,title,status,managerId,combinationSchemaId,salesStartAt,salesEndAt,drawAt,test,createdAt,version\n");
        for (DrawDto item : items) {
            csv.append(csv(item.id()))
                    .append(',')
                    .append(csv(item.title()))
                    .append(',')
                    .append(csv(item.status()))
                    .append(',')
                    .append(csv(item.managerId()))
                    .append(',')
                    .append(csv(item.combinationSchemaId()))
                    .append(',')
                    .append(csv(item.salesStartAt()))
                    .append(',')
                    .append(csv(item.salesEndAt()))
                    .append(',')
                    .append(csv(item.drawAt()))
                    .append(',')
                    .append(item.test())
                    .append(',')
                    .append(csv(item.createdAt()))
                    .append(',')
                    .append(item.version())
                    .append('\n');
        }
        return csv.toString();
    }

    private String ticketCsv(List<TicketDto> items) {
        StringBuilder csv = new StringBuilder("id,userId,drawId,status,priceAmount,priceCurrency,test,createdAt,version\n");
        for (TicketDto item : items) {
            csv.append(csv(item.id()))
                    .append(',')
                    .append(csv(item.userId()))
                    .append(',')
                    .append(csv(item.drawId()))
                    .append(',')
                    .append(csv(item.status()))
                    .append(',')
                    .append(csv(item.priceAmount()))
                    .append(',')
                    .append(csv(item.priceCurrency()))
                    .append(',')
                    .append(item.test())
                    .append(',')
                    .append(csv(item.createdAt()))
                    .append(',')
                    .append(item.version())
                    .append('\n');
        }
        return csv.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String string = String.valueOf(value);
        return "\"" + string.replace("\"", "\"\"") + "\"";
    }

    private UUID uuidQuery(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private Instant instantQuery(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private DrawStatus drawStatusQuery(HttpServletRequest request) {
        String value = request.getParameter("status");
        return value == null || value.isBlank() ? null : DrawStatus.valueOf(value);
    }

    private TicketStatus ticketStatusQuery(HttpServletRequest request) {
        String value = request.getParameter("status");
        return value == null || value.isBlank() ? null : TicketStatus.valueOf(value);
    }

    private int intQuery(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public record DrawReportResponse(List<DrawDto> items, long total, int limit, int offset, boolean hasMore) {
        public DrawReportResponse {
            items = List.copyOf(items);
        }
    }

    public record TicketReportResponse(List<TicketDto> items, long total, int limit, int offset, boolean hasMore) {
        public TicketReportResponse {
            items = List.copyOf(items);
        }
    }
}
