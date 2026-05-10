package com.lottery.infrastructure.lottery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class JsonCombinationEngineTest {
    @Test
    void generatedCombinationRespectsNoDuplicatesSchema() {
        JsonCombinationEngine engine = new JsonCombinationEngine(new ObjectMapper());
        CombinationSchema schema = new CombinationSchema(
                UUID.randomUUID(),
                "two unique numbers",
                new CombinationSchemaDefinition("""
                        {
                          "allowDuplicates": false,
                          "positions": [
                            { "type": "NUMBER", "min": 1, "max": 2 },
                            { "type": "NUMBER", "min": 1, "max": 2 }
                          ]
                        }
                        """),
                Instant.parse("2026-05-01T00:00:00Z"));

        var generated = engine.generate(schema);

        assertEquals(2, generated.combination().values().size());
        assertEquals(2, generated.combination().values().stream().distinct().count());
    }
}
