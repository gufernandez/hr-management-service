package com.banco.hr.domain.model.employee;

import com.banco.hr.domain.exception.DomainException;

public record EmployeeName(String value) {

    public EmployeeName {
        if (value == null || value.isBlank()) {
            throw new DomainException("O nome do colaborador não pode ser vazio.");
        }

        value = value.trim();
    }
}
