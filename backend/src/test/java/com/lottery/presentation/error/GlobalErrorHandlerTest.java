package com.lottery.presentation.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.lottery.infrastructure.config.JsonMapperFactory;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GlobalErrorHandlerTest {
    @Test
    void jsonMappingErrorsReturnValidationError() throws Exception {
        ObjectMapper objectMapper = new JsonMapperFactory().create();
        InvalidFormatException exception = org.junit.jupiter.api.Assertions.assertThrows(
                InvalidFormatException.class,
                () -> objectMapper.readValue("{\"id\":\"11111\"}", UuidRequest.class));
        ResponseCapture response = new ResponseCapture();

        new GlobalErrorHandler(objectMapper).handle(request(), response.proxy(), exception);

        assertEquals(400, response.status());
        assertEquals("application/json", response.contentType());
        String body = response.body();
        assertTrue(body.contains("\"code\":\"VALIDATION_ERROR\""));
        assertTrue(body.contains("\"message\":\"Malformed JSON request\""));
    }

    private HttpServletRequest request() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) {
                        return null;
                    }
                    return null;
                });
    }

    private record UuidRequest(UUID id) {
    }

    private static final class ResponseCapture {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private int status;
        private String contentType;

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) args[0];
                            yield null;
                        }
                        case "getOutputStream" -> outputStream();
                        default -> null;
                    });
        }

        int status() {
            return status;
        }

        String contentType() {
            return contentType;
        }

        String body() {
            return body.toString(java.nio.charset.StandardCharsets.UTF_8);
        }

        private ServletOutputStream outputStream() {
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int value) {
                    body.write(value);
                }
            };
        }
    }
}
