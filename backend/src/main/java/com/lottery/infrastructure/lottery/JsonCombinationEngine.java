package com.lottery.infrastructure.lottery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.port.lottery.CombinationEvaluatorPort;
import com.lottery.application.port.lottery.CombinationValidatorPort;
import com.lottery.application.port.lottery.WinningCombinationGeneratorPort;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.valueobject.Combination;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonCombinationEngine implements WinningCombinationGeneratorPort, CombinationEvaluatorPort, CombinationValidatorPort {
    private static final String ALGORITHM_VERSION = "json-schema-secure-random-v1";
    private static final String RANDOM_PROVIDER = "SecureRandom";

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    public JsonCombinationEngine(ObjectMapper objectMapper) {
        this(objectMapper, new SecureRandom());
    }

    JsonCombinationEngine(ObjectMapper objectMapper, SecureRandom secureRandom) {
        this.objectMapper = objectMapper;
        this.secureRandom = secureRandom;
    }

    @Override
    public GeneratedWinningCombination generate(CombinationSchema schema) {
        try {
            JsonNode root = objectMapper.readTree(schema.definition().document());
            JsonNode positions = root.path("positions");
            if (!positions.isArray() || positions.isEmpty()) {
                throw new IllegalArgumentException("Combination schema positions must be a non-empty array");
            }
            boolean allowDuplicates = root.path("allowDuplicates").asBoolean(true);
            List<String> values = new ArrayList<>();
            Set<String> usedValues = new HashSet<>();
            for (JsonNode position : positions) {
                String value = generateUniqueValue(position, allowDuplicates, usedValues);
                values.add(value);
                usedValues.add(value);
            }
            Combination combination = new Combination(values);
            return new GeneratedWinningCombination(
                    combination,
                    ALGORITHM_VERSION,
                    RANDOM_PROVIDER,
                    proofHash(schema.id() + ":" + String.join("|", values) + ":" + Instant.now()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to generate winning combination", exception);
        }
    }

    @Override
    public void validate(Combination combination, CombinationSchema schema) {
        try {
            JsonNode root = objectMapper.readTree(schema.definition().document());
            JsonNode positions = root.path("positions");
            if (!positions.isArray() || positions.isEmpty()) {
                throw new IllegalArgumentException("Combination schema positions must be a non-empty array");
            }
            List<String> values = combination.values();
            if (values.size() != positions.size()) {
                throw new IllegalArgumentException("Combination length does not match schema");
            }
            if (!root.path("allowDuplicates").asBoolean(true) && values.stream().distinct().count() != values.size()) {
                throw new IllegalArgumentException("Combination duplicates are not allowed");
            }
            for (int index = 0; index < positions.size(); index++) {
                validateValue(values.get(index), positions.get(index));
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to validate combination", exception);
        }
    }

    @Override
    public BigDecimal matchPercent(Combination ticketCombination, Combination winningCombination, CombinationSchema schema) {
        try {
            JsonNode root = objectMapper.readTree(schema.definition().document());
            boolean orderSensitive = !root.has("orderSensitive") || root.path("orderSensitive").asBoolean(true);
            List<String> ticketValues = ticketCombination.values();
            List<String> winningValues = winningCombination.values();
            if (winningValues.isEmpty() || ticketValues.size() != winningValues.size()) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            int matches = orderSensitive
                    ? countOrderedMatches(ticketValues, winningValues)
                    : countUnorderedMatches(ticketValues, winningValues);
            return BigDecimal.valueOf(matches)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(winningValues.size()), 2, RoundingMode.HALF_UP);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to evaluate combination", exception);
        }
    }

    private void validateValue(String value, JsonNode position) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Combination value must not be blank");
        }
        String type = position.path("type").asText();
        switch (type) {
            case "NUMBER" -> {
                int min = position.path("min").asInt(1);
                int max = position.path("max").asInt(99);
                int parsed;
                try {
                    parsed = Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("NUMBER combination value must be an integer", exception);
                }
                if (parsed < min || parsed > max) {
                    throw new IllegalArgumentException("NUMBER combination value is outside allowed range");
                }
            }
            case "LETTER" -> {
                String alphabet = position.path("alphabet").asText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                if ("unicode".equalsIgnoreCase(alphabet)) {
                    if (value.codePointCount(0, value.length()) != 1) {
                        throw new IllegalArgumentException("LETTER combination value must contain one symbol");
                    }
                    return;
                }
                if (!alphabet.contains(value)) {
                    throw new IllegalArgumentException("LETTER combination value is outside allowed alphabet");
                }
            }
            case "EMOJI", "TEXT", "IMAGE" -> {
            }
            default -> throw new IllegalArgumentException("Unsupported combination position type: " + type);
        }
    }

    private String generateValue(JsonNode position) {
        String type = position.path("type").asText();
        return switch (type) {
            case "NUMBER" -> {
                int min = position.path("min").asInt(1);
                int max = position.path("max").asInt(99);
                if (max < min) {
                    throw new IllegalArgumentException("NUMBER position max must be >= min");
                }
                yield Integer.toString(secureRandom.nextInt(max - min + 1) + min);
            }
            case "LETTER" -> {
                String alphabet = position.path("alphabet").asText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                String effectiveAlphabet = "unicode".equalsIgnoreCase(alphabet) ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : alphabet;
                if (effectiveAlphabet.isBlank()) {
                    throw new IllegalArgumentException("LETTER alphabet must not be blank");
                }
                int index = secureRandom.nextInt(effectiveAlphabet.length());
                yield String.valueOf(effectiveAlphabet.charAt(index));
            }
            case "EMOJI" -> "EMOJI_DEFAULT_" + secureRandom.nextInt(1, 10);
            case "TEXT" -> "TEXT_" + secureRandom.nextInt(1, 1000);
            case "IMAGE" -> "IMAGE:" + position.path("allowedAssetGroupId").asText("default");
            default -> throw new IllegalArgumentException("Unsupported combination position type: " + type);
        };
    }

    private String generateUniqueValue(JsonNode position, boolean allowDuplicates, Set<String> usedValues) {
        for (int attempt = 0; attempt < 1024; attempt++) {
            String value = generateValue(position);
            if (allowDuplicates || !usedValues.contains(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to generate unique combination value for schema position");
    }

    private int countOrderedMatches(List<String> left, List<String> right) {
        int matches = 0;
        for (int index = 0; index < right.size(); index++) {
            if (right.get(index).equals(left.get(index))) {
                matches++;
            }
        }
        return matches;
    }

    private int countUnorderedMatches(List<String> left, List<String> right) {
        Map<String, Integer> counts = new HashMap<>();
        for (String value : right) {
            counts.merge(value, 1, Integer::sum);
        }
        int matches = 0;
        for (String value : left) {
            Integer count = counts.get(value);
            if (count != null && count > 0) {
                matches++;
                counts.put(value, count - 1);
            }
        }
        return matches;
    }

    private String proofHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
