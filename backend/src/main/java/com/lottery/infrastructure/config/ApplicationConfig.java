package com.lottery.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.application.usecase.draw.ListDrawsUseCase;
import com.lottery.application.usecase.auth.LoginByPasswordUseCase;
import com.lottery.application.usecase.auth.RegisterUserUseCase;
import com.lottery.application.usecase.ticket.CreateTicketUseCase;
import com.lottery.application.usecase.ticket.ListTicketsUseCase;
import com.lottery.application.usecase.user.CreateUserUseCase;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.infrastructure.openapi.OpenApiResource;
import com.lottery.infrastructure.persistence.jdbc.JdbcDrawRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcInvoiceRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcPaymentRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcRbacRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTicketRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTransactionManager;
import com.lottery.infrastructure.persistence.jdbc.JdbcUserRepository;
import com.lottery.infrastructure.security.BcryptPasswordHasher;
import com.lottery.infrastructure.security.DatabaseAuthorizationAdapter;
import com.lottery.infrastructure.security.InMemorySessionTokenService;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.middleware.RequestContextFilter;
import com.lottery.presentation.rest.OpenApiServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
import com.lottery.presentation.rest.auth.LoginServlet;
import com.lottery.presentation.rest.auth.RegisterServlet;
import com.lottery.presentation.rest.draw.CreateDrawServlet;
import com.lottery.presentation.rest.health.HealthServlet;
import com.lottery.presentation.rest.health.ReadyServlet;
import com.lottery.presentation.rest.ticket.CreateTicketServlet;
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
        InvoiceRepository invoiceRepository = new JdbcInvoiceRepository(transactionManager);
        PaymentRepository paymentRepository = new JdbcPaymentRepository(transactionManager);
        RbacRepository rbacRepository = new JdbcRbacRepository(transactionManager);

        AuthorizationPort authorizationPort = new DatabaseAuthorizationAdapter(rbacRepository);
        PasswordHasher passwordHasher = new BcryptPasswordHasher(properties.bcryptCost());
        InMemorySessionTokenService tokenService = new InMemorySessionTokenService(properties.accessTokenTtlSeconds());

        CreateUserUseCase createUserUseCase = new CreateUserUseCase(
                userRepository,
                authorizationPort,
                passwordHasher,
                transactionManager,
                clock,
                new UserMapper());
        RegisterUserUseCase registerUserUseCase = new RegisterUserUseCase(
                userRepository,
                rbacRepository,
                passwordHasher,
                transactionManager,
                clock,
                new UserMapper());
        LoginByPasswordUseCase loginByPasswordUseCase = new LoginByPasswordUseCase(
                userRepository,
                passwordHasher,
                tokenService,
                transactionManager,
                new UserMapper());
        CreateDrawUseCase createDrawUseCase = new CreateDrawUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                clock,
                new DrawMapper());
        ListDrawsUseCase listDrawsUseCase = new ListDrawsUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                new DrawMapper());
        CreateTicketUseCase createTicketUseCase = new CreateTicketUseCase(
                userRepository,
                drawRepository,
                ticketRepository,
                authorizationPort,
                transactionManager,
                clock,
                new TicketPurchasePolicy(),
                new TicketMapper());
        ListTicketsUseCase listTicketsUseCase = new ListTicketsUseCase(
                ticketRepository,
                authorizationPort,
                transactionManager,
                new TicketMapper());

        ServletUseCaseContextFactory contextFactory = new ServletUseCaseContextFactory(tokenService);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addFilter(new FilterHolder(new RequestContextFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HealthServlet(objectMapper, errorHandler)), "/health");
        context.addServlet(new ServletHolder(new ReadyServlet(objectMapper, errorHandler, dataSource)), "/ready");
        context.addServlet(new ServletHolder(new OpenApiServlet(new OpenApiResource())), "/api/v1/openapi.yaml");
        context.addServlet(
                new ServletHolder(new RegisterServlet(objectMapper, errorHandler, registerUserUseCase)),
                "/api/v1/auth/register");
        context.addServlet(
                new ServletHolder(new LoginServlet(objectMapper, errorHandler, loginByPasswordUseCase)),
                "/api/v1/auth/login");
        context.addServlet(
                new ServletHolder(new CreateUserServlet(objectMapper, errorHandler, createUserUseCase, contextFactory)),
                "/api/v1/users");
        context.addServlet(
                new ServletHolder(new CreateDrawServlet(objectMapper, errorHandler, createDrawUseCase, listDrawsUseCase, contextFactory)),
                "/api/v1/draws");
        context.addServlet(
                new ServletHolder(
                        new CreateTicketServlet(objectMapper, errorHandler, createTicketUseCase, listTicketsUseCase, contextFactory)),
                "/api/v1/tickets");

        Server server = new Server(properties.httpPort());
        server.setHandler(context);
        server.setStopAtShutdown(true);
        return server;
    }
}
