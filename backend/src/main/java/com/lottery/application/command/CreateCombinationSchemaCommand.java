package com.lottery.application.command;

import java.util.Objects;

public record CreateCombinationSchemaCommand(String name, String definitionJson) {
    public CreateCombinationSchemaCommand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(definitionJson, "definitionJson");
    }
}
