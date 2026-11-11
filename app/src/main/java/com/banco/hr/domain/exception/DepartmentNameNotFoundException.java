package com.banco.hr.domain.exception;

public class DepartmentNameNotFoundException extends DomainException {

    public DepartmentNameNotFoundException(String departmentName) {
        super("Departamento '%s' não encontrado.".formatted(departmentName));
    }
}
