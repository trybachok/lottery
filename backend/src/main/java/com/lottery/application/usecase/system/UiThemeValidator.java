package com.lottery.application.usecase.system;

import com.lottery.application.ValidationException;
import java.util.Map;

final class UiThemeValidator {
    void validate(Map<String, Object> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new ValidationException("Theme tokens must not be empty");
        }
        requireString(tokens, "mode");
        Object colors = tokens.get("colors");
        if (!(colors instanceof Map<?, ?> colorMap) || colorMap.isEmpty()) {
            throw new ValidationException("Theme tokens must contain colors object");
        }
        requireColor(colorMap, "background");
        requireColor(colorMap, "surface");
        requireColor(colorMap, "text");
        requireColor(colorMap, "primary");
    }

    private void requireString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ValidationException("Theme tokens must contain " + key);
        }
    }

    private void requireColor(Map<?, ?> colors, String key) {
        Object value = colors.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ValidationException("Theme colors must contain " + key);
        }
    }
}
