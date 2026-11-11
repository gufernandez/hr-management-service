package com.banco.hr.domain.model.department;

import java.util.Objects;
import java.util.UUID;

public record DepartmentId(UUID value) {

    public DepartmentId {
        Objects.requireNonNull(value, "O identificador do departamento é obrigatório.");
    }

    public static DepartmentId generate() {
        return new DepartmentId(UUID.randomUUID());
    }
}
