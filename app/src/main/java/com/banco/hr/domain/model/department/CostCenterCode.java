package com.banco.hr.domain.model.department;

import com.banco.hr.domain.exception.DomainException;

public record CostCenterCode(String value) {

    public CostCenterCode {
        if (value == null || value.isBlank()) {
            throw new DomainException("O código do centro de custo não pode ser vazio.");
        }

        value = value.trim();
    }
}
