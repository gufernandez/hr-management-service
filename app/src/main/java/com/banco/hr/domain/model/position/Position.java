package com.banco.hr.domain.model.position;

import java.time.Instant;
import java.util.Objects;

public final class Position {

    private final PositionId id;
    private final PositionTitle title;
    private final PositionLevel level;
    private final Instant createdAt;

    private Position(
            PositionId id,
            PositionTitle title,
            PositionLevel level,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "O identificador do cargo é obrigatório.");
        this.title = Objects.requireNonNull(title, "O título do cargo é obrigatório.");
        this.level = Objects.requireNonNull(level, "O nível do cargo é obrigatório.");
        this.createdAt = Objects.requireNonNull(
                createdAt, "A data de criação do cargo é obrigatória.");
    }

    public static Position create(PositionTitle title, PositionLevel level, Instant createdAt) {
        return new Position(PositionId.generate(), title, level, createdAt);
    }

    public static Position reconstitute(
            PositionId id,
            PositionTitle title,
            PositionLevel level,
            Instant createdAt) {
        return new Position(id, title, level, createdAt);
    }

    public PositionId id() {
        return id;
    }

    public PositionTitle title() {
        return title;
    }

    public PositionLevel level() {
        return level;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
