package com.banco.hr.domain.model.employee;

import java.util.Objects;
import java.util.UUID;

public record EmployeeId(UUID value) {

    public EmployeeId {
        Objects.requireNonNull(value, "O identificador do colaborador é obrigatório.");
    }

    public static EmployeeId generate() {
        return new EmployeeId(UUID.randomUUID());
    }
}
