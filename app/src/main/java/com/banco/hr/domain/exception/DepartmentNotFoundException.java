package com.banco.hr.domain.exception;

import java.util.UUID;

public class DepartmentNotFoundException extends DomainException {

    public DepartmentNotFoundException(UUID departmentId) {
        super("Departamento '%s' não encontrado.".formatted(departmentId));
    }
}
