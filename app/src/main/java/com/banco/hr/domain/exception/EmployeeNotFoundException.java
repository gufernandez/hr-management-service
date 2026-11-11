package com.banco.hr.domain.exception;

import java.util.UUID;

public class EmployeeNotFoundException extends DomainException {

    public EmployeeNotFoundException(UUID employeeId) {
        super("Colaborador '%s' não encontrado.".formatted(employeeId));
    }
}
