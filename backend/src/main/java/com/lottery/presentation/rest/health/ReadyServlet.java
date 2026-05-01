package com.lottery.presentation.rest.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.presentation.error.GlobalErrorHandler;
import com.lottery.presentation.rest.JsonServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;

public final class ReadyServlet extends JsonServlet {
    private final DataSource dataSource;

    public ReadyServlet(ObjectMapper objectMapper, GlobalErrorHandler errorHandler, DataSource dataSource) {
        super(objectMapper, errorHandler);
        this.dataSource = dataSource;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.execute();
            writeJson(response, 200, java.util.Map.of("status", "READY"));
        } catch (Exception exception) {
            writeJson(response, 503, java.util.Map.of("status", "NOT_READY"));
        }
    }
}
