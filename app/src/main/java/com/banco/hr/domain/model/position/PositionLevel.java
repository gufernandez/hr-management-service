package com.banco.hr.domain.model.position;

import com.banco.hr.domain.exception.DomainException;

public record PositionLevel(String value) {

    public PositionLevel {
        if (value == null || value.isBlank()) {
            throw new DomainException("O nível do cargo não pode ser vazio.");
        }

        value = PositionText.toTitleCase(value);
    }
}
