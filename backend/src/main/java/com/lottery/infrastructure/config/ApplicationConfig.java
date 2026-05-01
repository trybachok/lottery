package com.lottery.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.mapper.DrawMapper;
import com.lottery.application.mapper.TicketMapper;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.usecase.draw.CreateDrawUseCase;
import com.lottery.application.usecase.ticket.CreateTicketUseCase;
import com.lottery.application.usecase.user.CreateUserUseCase;
import com.lottery.domain.policy.TicketPurchasePolicy;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.infrastructure.openapi.OpenApiResource;
import com.lottery.infrastructure.persistence.jdbc.JdbcDrawRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTicketRepository;
import com.lottery.infrastructure.persistence.jdbc.JdbcTransactionManager;
import com.lottery.infrastructure.persistence.jdbc.JdbcUserRepository;
import com.lottery.infrastructure.security.BcryptPasswordHasher;
import com.lottery.infrastructure.security.ContextAuthorizationAdapter;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.middleware.RequestContextFilter;
import com.lottery.presentation.rest.OpenApiServlet;
import com.lottery.presentation.rest.ServletUseCaseContextFactory;
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
        AuthorizationPort authorizationPort = new ContextAuthorizationAdapter();
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);

        UserRepository userRepository = new JdbcUserRepository(transactionManager);
        DrawRepository drawRepository = new JdbcDrawRepository(transactionManager);
        TicketRepository ticketRepository = new JdbcTicketRepository(transactionManager, objectMapper);

        CreateUserUseCase createUserUseCase = new CreateUserUseCase(
                userRepository,
                authorizationPort,
                new BcryptPasswordHasher(properties.bcryptCost()),
                transactionManager,
                clock,
                new UserMapper());
        CreateDrawUseCase createDrawUseCase = new CreateDrawUseCase(
                drawRepository,
                authorizationPort,
                transactionManager,
                clock,
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

        ServletUseCaseContextFactory contextFactory = new ServletUseCaseContextFactory();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addFilter(new FilterHolder(new RequestContextFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HealthServlet(objectMapper, errorHandler)), "/health");
        context.addServlet(new ServletHolder(new ReadyServlet(objectMapper, errorHandler, dataSource)), "/ready");
        context.addServlet(new ServletHolder(new OpenApiServlet(new OpenApiResource())), "/api/v1/openapi.yaml");
        context.addServlet(
                new ServletHolder(new CreateUserServlet(objectMapper, errorHandler, createUserUseCase, contextFactory)),
                "/api/v1/users");
        context.addServlet(
                new ServletHolder(new CreateDrawServlet(objectMapper, errorHandler, createDrawUseCase, contextFactory)),
                "/api/v1/draws");
        context.addServlet(
                new ServletHolder(new CreateTicketServlet(objectMapper, errorHandler, createTicketUseCase, contextFactory)),
                "/api/v1/tickets");

        Server server = new Server(properties.httpPort());
        server.setHandler(context);
        server.setStopAtShutdown(true);
        return server;
    }
}
