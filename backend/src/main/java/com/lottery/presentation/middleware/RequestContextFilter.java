package com.lottery.presentation.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestContextFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestId = headerOrNew(httpRequest, "X-Request-Id");
        String correlationId = httpRequest.getHeader("X-Correlation-Id");
        RequestContext context = new RequestContext(requestId, correlationId, System.nanoTime());
        httpRequest.setAttribute(RequestContext.ATTRIBUTE_NAME, context);
        httpResponse.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - context.startedAtNanos()) / 1_000_000L;
            log.info(
                    "requestId={} correlationId={} method={} path={} status={} durationMs={}",
                    requestId,
                    correlationId,
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus(),
                    durationMillis);
        }
    }

    @Override
    public void destroy() {
    }

    private String headerOrNew(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? "req_" + UUID.randomUUID() : value;
    }
}
