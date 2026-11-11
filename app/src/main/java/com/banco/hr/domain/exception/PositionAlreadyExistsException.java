package com.banco.hr.domain.exception;

public class PositionAlreadyExistsException extends DomainException {

    public PositionAlreadyExistsException(String title, String level) {
        super("Já existe um cargo com o título '%s' e nível '%s'.".formatted(title, level));
    }
}
