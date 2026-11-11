package com.banco.hr.domain.model.position;

import java.util.Objects;
import java.util.UUID;

public record PositionId(UUID value) {

    public PositionId {
        Objects.requireNonNull(value, "O identificador do cargo é obrigatório.");
    }

    public static PositionId generate() {
        return new PositionId(UUID.randomUUID());
    }
}
