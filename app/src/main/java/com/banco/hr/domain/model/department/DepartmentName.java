package com.banco.hr.domain.model.department;

import com.banco.hr.domain.exception.DomainException;

public record DepartmentName(String value) {

    public DepartmentName {
        if (value == null || value.isBlank()) {
            throw new DomainException("O nome do departamento não pode ser vazio.");
        }

        value = value.trim();
    }
}
