package com.banco.hr.domain.exception;

public class EmployeeAlreadyExistsException extends DomainException {

    public EmployeeAlreadyExistsException(String email) {
        super("Já existe um colaborador com o e-mail '%s'.".formatted(email));
    }
}
