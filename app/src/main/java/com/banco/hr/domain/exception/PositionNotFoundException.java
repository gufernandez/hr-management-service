package com.banco.hr.domain.exception;

import java.util.UUID;

public class PositionNotFoundException extends DomainException {

    public PositionNotFoundException(UUID positionId) {
        super("Cargo '%s' não encontrado.".formatted(positionId));
    }
}
