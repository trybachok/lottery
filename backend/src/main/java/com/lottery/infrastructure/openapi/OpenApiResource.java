package com.lottery.infrastructure.openapi;

import com.lottery.application.port.openapi.OpenApiDocumentPort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class OpenApiResource implements OpenApiDocumentPort {
    @Override
    public String load() {
        try (InputStream inputStream = getClass().getResourceAsStream("/openapi/openapi.yaml")) {
            if (inputStream == null) {
                throw new IllegalStateException("OpenAPI resource not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load OpenAPI resource", exception);
        }
    }
}
