package com.lottery.presentation.rest.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.NotFoundException;
import com.lottery.application.command.BulkCreateTicketsCommand;
import com.lottery.application.command.CreateInvoiceForTicketCommand;
import com.lottery.application.command.CreateTicketCommand;
import com.lottery.application.dto.InvoiceDto;
import com.lottery.application.dto.TicketDto;
import com.lottery.application.dto.TicketListDto;
import com.lottery.application.usecase.payment.CreateInvoiceForTicketUseCase;
import com.lottery.application.usecase.payment.GetTicketInvoiceUseCase;
import com.lottery.application.usecase.ticket.BulkCreateTicketsUseCase;
import com.lottery.application.usecase.ticket.CancelTicketUseCase;
import com.lottery.application.usecase.ticket.CheckTicketResultUseCase;
import com.lottery.application.usecase.ticket.DeleteTicketUseCase;
import com.lottery.application.usecase.ticket.GetTicketUseCase;
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

public final class TicketItemServlet extends JsonServlet {
    private final GetTicketUseCase getTicketUseCase;
    private final BulkCreateTicketsUseCase bulkCreateTicketsUseCase;
    private final CancelTicketUseCase cancelTicketUseCase;
    private final DeleteTicketUseCase deleteTicketUseCase;
    private final CheckTicketResultUseCase checkTicketResultUseCase;
    private final CreateInvoiceForTicketUseCase createInvoiceUseCase;
    private final GetTicketInvoiceUseCase getTicketInvoiceUseCase;
    private final ServletUseCaseContextFactory contextFactory;

    public TicketItemServlet(
            ObjectMapper objectMapper,
            GlobalErrorHandler errorHandler,
            GetTicketUseCase getTicketUseCase,
            BulkCreateTicketsUseCase bulkCreateTicketsUseCase,
            CancelTicketUseCase cancelTicketUseCase,
            DeleteTicketUseCase deleteTicketUseCase,
            CheckTicketResultUseCase checkTicketResultUseCase,
            CreateInvoiceForTicketUseCase createInvoiceUseCase,
            GetTicketInvoiceUseCase getTicketInvoiceUseCase,
            ServletUseCaseContextFactory contextFactory) {
        super(objectMapper, errorHandler);
        this.getTicketUseCase = getTicketUseCase;
        this.bulkCreateTicketsUseCase = bulkCreateTicketsUseCase;
        this.cancelTicketUseCase = cancelTicketUseCase;
        this.deleteTicketUseCase = deleteTicketUseCase;
        this.checkTicketResultUseCase = checkTicketResultUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.getTicketInvoiceUseCase = getTicketInvoiceUseCase;
        this.contextFactory = contextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if ("invoice".equals(path.action()) && path.ticketId() != null) {
                writeJson(response, 200, getTicketInvoiceUseCase.execute(path.ticketId(), contextFactory.from(request)));
                return;
            }
            if (path.action() != null || path.ticketId() == null) {
                throw new NotFoundException("Endpoint");
            }
            TicketDto result = getTicketUseCase.execute(path.ticketId(), contextFactory.from(request));
            writeJson(response, 200, result);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if ("bulk".equals(path.action()) && path.ticketId() == null) {
                BulkCreateTicketsRequest body = readJson(request, BulkCreateTicketsRequest.class);
                TicketListDto result = bulkCreateTicketsUseCase.execute(
                        new BulkCreateTicketsCommand(body.tickets().stream().map(TicketItemServlet::command).toList()),
                        contextFactory.from(request));
                writeJson(response, 201, result);
                return;
            }
            if (path.ticketId() == null || path.action() == null) {
                throw new NotFoundException("Endpoint");
            }
            switch (path.action()) {
                case "cancel" -> writeJson(response, 200, cancelTicketUseCase.execute(path.ticketId(), contextFactory.from(request)));
                case "check" -> writeJson(response, 200, checkTicketResultUseCase.execute(path.ticketId(), contextFactory.from(request)));
                case "invoice" -> {
                    CreateInvoiceRequest body = readJson(request, CreateInvoiceRequest.class);
                    InvoiceDto result = createInvoiceUseCase.execute(
                            new CreateInvoiceForTicketCommand(path.ticketId(), body.providerCode(), body.idempotencyKey()),
                            contextFactory.from(request));
                    writeJson(response, 201, result);
                }
                default -> throw new NotFoundException("Endpoint");
            }
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PathParts path = path(request);
            if (path.action() != null || path.ticketId() == null) {
                throw new NotFoundException("Endpoint");
            }
            deleteTicketUseCase.execute(path.ticketId(), contextFactory.from(request));
            response.setStatus(204);
        } catch (Exception exception) {
            handleException(request, response, exception);
        }
    }

    private static CreateTicketCommand command(CreateTicketRequest request) {
        return new CreateTicketCommand(
                request.userId(),
                request.drawId(),
                request.combinationValues(),
                request.priceAmount(),
                Currency.getInstance(request.priceCurrency()),
                request.test());
    }

    private PathParts path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new NotFoundException("Endpoint");
        }
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length == 1 && "bulk".equals(parts[0])) {
            return new PathParts(null, "bulk");
        }
        if (parts.length < 1 || parts.length > 2) {
            throw new NotFoundException("Endpoint");
        }
        return new PathParts(UUID.fromString(parts[0]), parts.length == 2 ? parts[1] : null);
    }

    public record CreateTicketRequest(
            UUID userId,
            UUID drawId,
            List<String> combinationValues,
            BigDecimal priceAmount,
            String priceCurrency,
            boolean test) {
    }

    public record BulkCreateTicketsRequest(List<CreateTicketRequest> tickets) {
        public BulkCreateTicketsRequest {
            tickets = tickets == null ? List.of() : List.copyOf(tickets);
        }
    }

    public record CreateInvoiceRequest(String providerCode, String idempotencyKey) {
    }

    private record PathParts(UUID ticketId, String action) {
    }
}
