package com.banco.hr.domain.exception;

public class DepartmentAlreadyExistsException extends DomainException {

    public DepartmentAlreadyExistsException(String departmentName) {
        super("Já existe um departamento com o nome '%s'.".formatted(departmentName));
    }
}
