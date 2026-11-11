package com.banco.hr.domain.model.position;

import com.banco.hr.domain.exception.DomainException;

public record PositionTitle(String value) {

    public PositionTitle {
        if (value == null || value.isBlank()) {
            throw new DomainException("O título do cargo não pode ser vazio.");
        }

        value = PositionText.toTitleCase(value);
    }
}
