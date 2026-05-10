package com.lottery.presentation.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class CorsFilter implements Filter {
    private final Set<String> allowedOrigins;
    private final boolean allowAnyOrigin;

    public CorsFilter(String allowedOrigins) {
        String value = allowedOrigins == null ? "" : allowedOrigins.trim();
        this.allowAnyOrigin = "*".equals(value);
        this.allowedOrigins = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank() && !"*".equals(origin))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && isAllowed(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", allowAnyOrigin ? "*" : origin);
            httpResponse.setHeader("Vary", "Origin");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,Idempotency-Key,X-Request-Id,X-Signature");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(String origin) {
        return allowAnyOrigin || allowedOrigins.contains(origin);
    }
}
