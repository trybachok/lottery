package com.lottery.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.audit.AuditService;
import com.lottery.application.mapper.AuditLogMapper;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.mapper.DrawResultMapper;
import com.lottery.application.mapper.InvoiceMapper;
import com.lottery.application.mapper.PaymentMapper;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.mapper.UiMapper;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.usecase.audit.ListAuditLogsUseCase;
import com.lottery.application.usecase.admin.AdminRbacUseCase;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.application.usecase.draw.ChangeDrawStatusUseCase;
import com.lottery.application.usecase.draw.AssignDrawManagerUseCase;
import com.lottery.application.usecase.draw.GenerateWinningCombinationUseCase;
import com.lottery.application.usecase.draw.GetDrawUseCase;
import com.lottery.application.usecase.draw.GetDrawResultUseCase;
import com.lottery.application.usecase.draw.ListDrawsUseCase;
import com.lottery.application.usecase.draw.RunDrawUseCase;
import com.lottery.application.usecase.draw.UpdateDrawUseCase;
import com.lottery.application.usecase.auth.LoginByPasswordUseCase;
import com.lottery.application.usecase.auth.RegisterUserUseCase;
import com.lottery.application.usecase.payment.CreateInvoiceForTicketUseCase;
import com.lottery.application.usecase.payment.CancelInvoiceUseCase;
import com.lottery.application.usecase.payment.ExpireInvoiceUseCase;
import com.lottery.application.usecase.payment.GetInvoiceUseCase;
import com.lottery.application.usecase.payment.GetTicketInvoiceUseCase;
import com.lottery.application.usecase.payment.ProcessPaymentOutboxUseCase;
import com.lottery.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.lottery.application.usecase.payment.RefundPaymentUseCase;
import com.lottery.application.usecase.report.GenerateDrawReportUseCase;
import com.lottery.application.usecase.report.GenerateTicketReportUseCase;
import com.lottery.application.usecase.system.GetOpenApiDocumentUseCase;
import com.lottery.application.usecase.system.AdminUiUseCase;
import com.lottery.application.usecase.system.GetHomePageUseCase;
import com.lottery.application.usecase.ticket.BulkCreateTicketsUseCase;
import com.lottery.application.usecase.ticket.CancelTicketUseCase;
import com.lottery.application.usecase.ticket.CheckTicketResultUseCase;
import com.lottery.application.usecase.ticket.CreateTicketUseCase;
import com.lottery.application.usecase.ticket.DeleteTicketUseCase;
import com.lottery.application.usecase.ticket.GetTicketUseCase;
import com.lottery.application.usecase.ticket.ListTicketsUseCase;
import com.lottery.application.usecase.ticket.TicketCreationService;
import com.lottery.application.usecase.user.CreateUserUseCase;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.policy.DrawStatusTransitionPolicy;
import com.lottery.domain.policy.TicketParticipationPolicy;
import com.lottery.domain.repository.AuditLogRepository;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.PaymentOutboxRepository;
import com.lottery.domain.repository.PaymentWebhookEventRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.SystemSettingsRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UiTemplateRepository;
import com.lottery.domain.repository.UiThemeRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.repository.WinningRuleRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.infrastructure.lottery.JsonCombinationEngine;
import com.lottery.infrastructure.openapi.OpenApiResource;
import com.lottery.infrastructure.payment.MockPaymentProviderAdapter;
import com.lottery.infrastructure.payment.PaymentOutboxWorker;
import com.lottery.infrastructure.persistence.jdbc.JdbcAuditLogRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcCombinationSchemaRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcDrawRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcDrawResultRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcInvoiceRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcPaymentRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcPaymentOutboxRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcPaymentWebhookEventRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcRbacRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcSystemSettingsRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTicketRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTransactionManager;
import com.lottery.infrastructure.persistence.jdbc.JdbcUiTemplateRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcUiThemeRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcUserRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcWinningRuleRepository;
import com.lottery.infrastructure.security.BcryptPasswordHasher;
import com.lottery.infrastructure.security.DatabaseAuthorizationAdapter;
import com.lottery.infrastructure.security.HmacTokenService;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.middleware.CorsFilter;
import com.lottery.presentation.middleware.RequestContextFilter;
import com.lottery.presentation.rest.OpenApiServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import com.lottery.presentation.rest.admin.AdminPermissionsServlet;
import com.lottery.presentation.rest.admin.AdminRolesServlet;
import com.lottery.presentation.rest.admin.AdminSettingsServlet;
import com.lottery.presentation.rest.admin.AdminUiTemplatesServlet;
import com.lottery.presentation.rest.admin.AdminUiThemesServlet;
import com.lottery.presentation.rest.admin.AdminUsersServlet;
import com.lottery.presentation.rest.admin.AssignDrawManagerServlet;
import com.lottery.presentation.rest.audit.AuditLogsServlet;
import com.lottery.presentation.rest.auth.LoginServlet;
import com.lottery.presentation.rest.auth.RegisterServlet;
import com.lottery.presentation.rest.draw.CreateDrawServlet;
import com.lottery.presentation.rest.draw.DrawItemServlet;
import com.lottery.presentation.rest.health.HealthServlet;
import com.lottery.presentation.rest.health.ReadyServlet;
import com.lottery.presentation.rest.payment.CreateInvoiceServlet;
import com.lottery.presentation.rest.payment.InvoiceItemServlet;
import com.lottery.presentation.rest.payment.PaymentWebhookServlet;
import com.lottery.presentation.rest.payment.RefundPaymentServlet;
import com.lottery.presentation.rest.report.ReportsServlet;
import com.lottery.presentation.rest.system.HomePageServlet;
import com.lottery.presentation.rest.ticket.CreateTicketServlet;
import com.lottery.presentation.rest.ticket.TicketItemServlet;
import com.lottery.presentation.rest.user.CreateUserServlet;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import javax.sql.DataSource;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

public final class ApplicationConfig {
    public Server createServer(ApplicationProperties properties) {
        ObjectMapper objectMapper = new JsonMapperFactory().create();
        GlobalErrorHandler errorHandler = new GlobalErrorHandler(objectMapper);
        DataSource dataSource = new DataSourceFactory().create(properties);
        if (properties.migrationsEnabled()) {
            new FlywayMigrationRunner().migrate(dataSource);
        }

        DomainClock clock = new SystemDomainClock();
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);

        UserRepository userRepository = new JdbcUserRepository(transactionManager);
        DrawRepository drawRepository = new JdbcDrawRepository(transactionManager);
        TicketRepository ticketRepository = new JdbcTicketRepository(transactionManager, objectMapper);
        CombinationSchemaRepository combinationSchemaRepository = new JdbcCombinationSchemaRepository(transactionManager);
        DrawResultRepository drawResultRepository = new JdbcDrawResultRepository(transactionManager, objectMapper);
        WinningRuleRepository winningRuleRepository = new JdbcWinningRuleRepository(transactionManager);
        InvoiceRepository invoiceRepository = new JdbcInvoiceRepository(transactionManager);
        PaymentRepository paymentRepository = new JdbcPaymentRepository(transactionManager);
        PaymentOutboxRepository paymentOutboxRepository = new JdbcPaymentOutboxRepository(transactionManager);
        PaymentWebhookEventRepository paymentWebhookEventRepository = new JdbcPaymentWebhookEventRepository(transactionManager);
        RbacRepository rbacRepository = new JdbcRbacRepository(transactionManager);
        AuditLogRepository auditLogRepository = new JdbcAuditLogRepository(transactionManager);
        UiThemeRepository uiThemeRepository = new JdbcUiThemeRepository(transactionManager, objectMapper);
        UiTemplateRepository uiTemplateRepository = new JdbcUiTemplateRepository(transactionManager, objectMapper);
        SystemSettingsRepository systemSettingsRepository = new JdbcSystemSettingsRepository(transactionManager, objectMapper);

        AuthorizationPort authorizationPort = new DatabaseAuthorizationAdapter(rbacRepository);
        AuditService auditService = new AuditService(auditLogRepository, rbacRepository, clock);
        PasswordHasher passwordHasher = new BcryptPasswordHasher(properties.bcryptCost());
        HmacTokenService tokenService = new HmacTokenService(properties.accessTokenSecret(), properties.accessTokenTtlSeconds());
        MockPaymentProviderAdapter paymentProvider = new MockPaymentProviderAdapter(properties.mockPaymentWebhookSecret());

        CreateUserUseCase createUserUseCase = new CreateUserUseCase(
                userRepository,
                authorizationPort,
                passwordHasher,
                transactionManager,
                clock,
                new UserMapper(),
                auditService);
        RegisterUserUseCase registerUserUseCase = new RegisterUserUseCase(
                userRepository,
                rbacRepository,
                passwordHasher,
                transactionManager,
                clock,
                new UserMapper());
        LoginByPasswordUseCase loginByPasswordUseCase = new LoginByPasswordUseCase(
                userRepository,
                rbacRepository,
                passwordHasher,
                tokenService,
                transactionManager,
                new UserMapper());
        CreateDrawUseCase createDrawUseCase = new CreateDrawUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                clock,
                new DrawMapper(),
                auditService);
        ListDrawsUseCase listDrawsUseCase = new ListDrawsUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                new DrawMapper());
        GetDrawUseCase getDrawUseCase = new GetDrawUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                new DrawMapper());
        UpdateDrawUseCase updateDrawUseCase = new UpdateDrawUseCase(
                drawRepository,
                drawResultRepository,
                authorizationPort,
                transactionManager,
                clock,
                new DrawMapper(),
                auditService);
        ChangeDrawStatusUseCase changeDrawStatusUseCase = new ChangeDrawStatusUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                new DrawStatusTransitionPolicy(),
                clock,
                new DrawMapper(),
                auditService);
        JsonCombinationEngine combinationEngine = new JsonCombinationEngine(objectMapper);
        GenerateWinningCombinationUseCase generateWinningCombinationUseCase = new GenerateWinningCombinationUseCase(
                drawRepository,
                combinationSchemaRepository,
                drawResultRepository,
                authorizationPort,
                transactionManager,
                combinationEngine,
                combinationEngine,
                new DrawStatusTransitionPolicy(),
                clock,
                new DrawResultMapper(),
                auditService);
        RunDrawUseCase runDrawUseCase = new RunDrawUseCase(
                drawRepository,
                combinationSchemaRepository,
                drawResultRepository,
                ticketRepository,
                winningRuleRepository,
                invoiceRepository,
                paymentRepository,
                authorizationPort,
                transactionManager,
                generateWinningCombinationUseCase,
                combinationEngine,
                new DrawStatusTransitionPolicy(),
                new TicketParticipationPolicy(),
                clock,
                auditService);
        GetDrawResultUseCase getDrawResultUseCase = new GetDrawResultUseCase(
                drawRepository,
                drawResultRepository,
                authorizationPort,
                transactionManager,
                new DrawResultMapper());
        TicketCreationService ticketCreationService = new TicketCreationService(
                userRepository,
                drawRepository,
                ticketRepository,
                combinationSchemaRepository,
                authorizationPort,
                combinationEngine,
                clock,
                new TicketPurchasePolicy());
        CreateTicketUseCase createTicketUseCase = new CreateTicketUseCase(
                transactionManager,
                ticketCreationService,
                new TicketMapper());
        BulkCreateTicketsUseCase bulkCreateTicketsUseCase = new BulkCreateTicketsUseCase(
                transactionManager,
                ticketCreationService,
                new TicketMapper());
        ListTicketsUseCase listTicketsUseCase = new ListTicketsUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                new TicketMapper());
        GetTicketUseCase getTicketUseCase = new GetTicketUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                new TicketMapper());
        CancelTicketUseCase cancelTicketUseCase = new CancelTicketUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                clock,
                new TicketMapper());
        DeleteTicketUseCase deleteTicketUseCase = new DeleteTicketUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                clock);
        CheckTicketResultUseCase checkTicketResultUseCase = new CheckTicketResultUseCase(
                ticketRepository,
                drawResultRepository,
                authorizationPort,
                transactionManager,
                new TicketMapper());
        CreateInvoiceForTicketUseCase createInvoiceForTicketUseCase = new CreateInvoiceForTicketUseCase(
                ticketRepository,
                invoiceRepository,
                paymentRepository,
                paymentOutboxRepository,
                authorizationPort,
                transactionManager,
                clock,
                new InvoiceMapper());
        GetInvoiceUseCase getInvoiceUseCase = new GetInvoiceUseCase(
                invoiceRepository,
                authorizationPort,
                transactionManager,
                new InvoiceMapper());
        GetTicketInvoiceUseCase getTicketInvoiceUseCase = new GetTicketInvoiceUseCase(
                ticketRepository,
                invoiceRepository,
                authorizationPort,
                transactionManager,
                new InvoiceMapper());
        CancelInvoiceUseCase cancelInvoiceUseCase = new CancelInvoiceUseCase(
                invoiceRepository,
                paymentRepository,
                ticketRepository,
                paymentOutboxRepository,
                authorizationPort,
                transactionManager,
                clock,
                new InvoiceMapper());
        ExpireInvoiceUseCase expireInvoiceUseCase = new ExpireInvoiceUseCase(cancelInvoiceUseCase);
        ProcessPaymentOutboxUseCase processPaymentOutboxUseCase = new ProcessPaymentOutboxUseCase(
                paymentOutboxRepository,
                invoiceRepository,
                paymentRepository,
                ticketRepository,
                paymentProvider,
                transactionManager,
                clock,
                objectMapper);
        ProcessPaymentWebhookUseCase processPaymentWebhookUseCase = new ProcessPaymentWebhookUseCase(
                paymentWebhookEventRepository,
                invoiceRepository,
                paymentRepository,
                ticketRepository,
                paymentProvider,
                transactionManager,
                clock,
                objectMapper);
        RefundPaymentUseCase refundPaymentUseCase = new RefundPaymentUseCase(
                paymentRepository,
                invoiceRepository,
                ticketRepository,
                paymentOutboxRepository,
                authorizationPort,
                transactionManager,
                clock,
                new PaymentMapper(),
                auditService);
        GenerateDrawReportUseCase generateDrawReportUseCase = new GenerateDrawReportUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                new DrawMapper(),
                auditService);
        GenerateTicketReportUseCase generateTicketReportUseCase = new GenerateTicketReportUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                new TicketMapper(),
                auditService);
        ListAuditLogsUseCase listAuditLogsUseCase = new ListAuditLogsUseCase(
                auditLogRepository,
                authorizationPort,
                transactionManager,
                new AuditLogMapper());
        AdminRbacUseCase adminRbacUseCase = new AdminRbacUseCase(
                userRepository,
                rbacRepository,
                authorizationPort,
                passwordHasher,
                transactionManager,
                clock,
                new UserMapper(),
                auditService);
        AssignDrawManagerUseCase assignDrawManagerUseCase = new AssignDrawManagerUseCase(
                drawRepository,
                userRepository,
                rbacRepository,
                authorizationPort,
                transactionManager,
                clock,
                new DrawMapper(),
                auditService);
        GetOpenApiDocumentUseCase getOpenApiDocumentUseCase = new GetOpenApiDocumentUseCase(
                new OpenApiResource(),
                authorizationPort,
                transactionManager);
        UiMapper uiMapper = new UiMapper();
        GetHomePageUseCase getHomePageUseCase = new GetHomePageUseCase(
                uiTemplateRepository,
                uiThemeRepository,
                systemSettingsRepository,
                transactionManager,
                uiMapper);
        AdminUiUseCase adminUiUseCase = new AdminUiUseCase(
                uiThemeRepository,
                uiTemplateRepository,
                systemSettingsRepository,
                authorizationPort,
                transactionManager,
                clock,
                uiMapper,
                auditService);

        ServletUseCaseContextFactory contextFactory = new ServletUseCaseContextFactory(tokenService, rbacRepository, transactionManager);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addFilter(new FilterHolder(new CorsFilter(properties.corsAllowedOrigins())), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(new FilterHolder(new RequestContextFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HealthServlet(objectMapper, errorHandler)), "/health");
        context.addServlet(new ServletHolder(new ReadyServlet(objectMapper, errorHandler, dataSource)), "/ready");
        context.addServlet(
                new ServletHolder(new OpenApiServlet(
                        objectMapper,
                        errorHandler,
                        getOpenApiDocumentUseCase,
                        contextFactory)),
                "/api/v1/openapi.yaml");
        context.addServlet(
                new ServletHolder(new RegisterServlet(objectMapper, errorHandler, registerUserUseCase)),
                "/api/v1/auth/register");
        context.addServlet(
                new ServletHolder(new LoginServlet(objectMapper, errorHandler, loginByPasswordUseCase)),
                "/api/v1/auth/login");
        context.addServlet(
                new ServletHolder(new HomePageServlet(objectMapper, errorHandler, getHomePageUseCase)),
                "/api/v1/home-page");
        context.addServlet(
                new ServletHolder(new CreateUserServlet(objectMapper, errorHandler, createUserUseCase, contextFactory)),
                "/api/v1/users");
        context.addServlet(
                new ServletHolder(new CreateDrawServlet(objectMapper, errorHandler, createDrawUseCase, listDrawsUseCase, contextFactory)),
                "/api/v1/draws");
        context.addServlet(
                new ServletHolder(new DrawItemServlet(
                        objectMapper,
                        errorHandler,
                        getDrawUseCase,
                        updateDrawUseCase,
                        changeDrawStatusUseCase,
                        generateWinningCombinationUseCase,
                        runDrawUseCase,
                        getDrawResultUseCase,
                        contextFactory)),
                "/api/v1/draws/*");
        context.addServlet(
                new ServletHolder(
                        new CreateTicketServlet(objectMapper, errorHandler, createTicketUseCase, listTicketsUseCase, contextFactory)),
                "/api/v1/tickets");
        context.addServlet(
                new ServletHolder(new TicketItemServlet(
                        objectMapper,
                        errorHandler,
                        getTicketUseCase,
                        bulkCreateTicketsUseCase,
                        cancelTicketUseCase,
                        deleteTicketUseCase,
                        checkTicketResultUseCase,
                        createInvoiceForTicketUseCase,
                        getTicketInvoiceUseCase,
                        contextFactory)),
                "/api/v1/tickets/*");
        context.addServlet(
                new ServletHolder(new PaymentWebhookServlet(objectMapper, errorHandler, processPaymentWebhookUseCase)),
                "/api/v1/payment-providers/*");
        context.addServlet(
                new ServletHolder(new InvoiceItemServlet(
                        objectMapper,
                        errorHandler,
                        getInvoiceUseCase,
                        cancelInvoiceUseCase,
                        expireInvoiceUseCase,
                        contextFactory)),
                "/api/v1/invoices/*");
        context.addServlet(
                new ServletHolder(new RefundPaymentServlet(objectMapper, errorHandler, refundPaymentUseCase, contextFactory)),
                "/api/v1/payments/*");
        context.addServlet(
                new ServletHolder(new ReportsServlet(
                        objectMapper,
                        errorHandler,
                        generateDrawReportUseCase,
                        generateTicketReportUseCase,
                        contextFactory)),
                "/api/v1/reports/*");
        context.addServlet(
                new ServletHolder(new AuditLogsServlet(objectMapper, errorHandler, listAuditLogsUseCase, contextFactory)),
                "/api/v1/admin/audit-logs");
        context.addServlet(
                new ServletHolder(new AdminUsersServlet(objectMapper, errorHandler, adminRbacUseCase, contextFactory)),
                "/api/v1/admin/users/*");
        context.addServlet(
                new ServletHolder(new AdminRolesServlet(objectMapper, errorHandler, adminRbacUseCase, contextFactory)),
                "/api/v1/admin/roles/*");
        context.addServlet(
                new ServletHolder(new AdminPermissionsServlet(objectMapper, errorHandler, adminRbacUseCase, contextFactory)),
                "/api/v1/admin/permissions/*");
        context.addServlet(
                new ServletHolder(new AdminUiThemesServlet(objectMapper, errorHandler, adminUiUseCase, contextFactory)),
                "/api/v1/admin/ui-themes/*");
        context.addServlet(
                new ServletHolder(new AdminUiTemplatesServlet(objectMapper, errorHandler, adminUiUseCase, contextFactory)),
                "/api/v1/admin/ui-templates/*");
        context.addServlet(
                new ServletHolder(new AdminSettingsServlet(objectMapper, errorHandler, adminUiUseCase, contextFactory)),
                "/api/v1/admin/settings/*");
        context.addServlet(
                new ServletHolder(new AssignDrawManagerServlet(objectMapper, errorHandler, assignDrawManagerUseCase, contextFactory)),
                "/api/v1/admin/draws/*");

        Server server = new Server(properties.httpPort());
        server.setHandler(context);
        server.addBean(new PaymentOutboxWorker(processPaymentOutboxUseCase, 50, 10));
        server.setStopAtShutdown(true);
        return server;
    }
}
