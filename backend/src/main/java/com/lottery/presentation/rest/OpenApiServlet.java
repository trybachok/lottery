package com.lottery.presentation.rest;

import com.lottery.infrastructure.openapi.OpenApiResource;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class OpenApiServlet extends HttpServlet {
    private final OpenApiResource openApiResource;

    public OpenApiServlet(OpenApiResource openApiResource) {
        this.openApiResource = openApiResource;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.setContentType("application/yaml");
        response.getWriter().write(openApiResource.load());
    }
}
