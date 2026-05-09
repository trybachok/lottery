package com.lottery.application.audit;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

final class AuditSnapshotJson {
    private AuditSnapshotJson() {
    }

    static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        StringBuilder json = new StringBuilder();
        append(json, value);
        return json.toString();
    }

    private static void append(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
            return;
        }
        if (value instanceof String stringValue) {
            appendString(json, stringValue);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
            return;
        }
        if (value instanceof UUID || value instanceof Instant || value instanceof Enum<?>) {
            appendString(json, value.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            appendMap(json, map);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            appendIterable(json, iterable);
            return;
        }
        appendString(json, value.toString());
    }

    private static void appendMap(StringBuilder json, Map<?, ?> map) {
        json.append('{');
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            appendString(json, String.valueOf(entry.getKey()));
            json.append(':');
            append(json, entry.getValue());
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append('}');
    }

    private static void appendIterable(StringBuilder json, Iterable<?> iterable) {
        json.append('[');
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            append(json, iterator.next());
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append(']');
    }

    private static void appendString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
