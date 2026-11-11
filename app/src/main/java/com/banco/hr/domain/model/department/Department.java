package com.banco.hr.domain.model.department;

import java.time.Instant;
import java.util.Objects;

public final class Department {

    private final DepartmentId id;
    private final DepartmentName name;
    private final CostCenterCode costCenterCode;
    private final Instant createdAt;

    private Department(
            DepartmentId id,
            DepartmentName name,
            CostCenterCode costCenterCode,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "O identificador do departamento é obrigatório.");
        this.name = Objects.requireNonNull(name, "O nome do departamento é obrigatório.");
        this.costCenterCode = Objects.requireNonNull(
                costCenterCode, "O código do centro de custo é obrigatório.");
        this.createdAt = Objects.requireNonNull(
                createdAt, "A data de criação do departamento é obrigatória.");
    }

    public static Department create(
            DepartmentName name,
            CostCenterCode costCenterCode,
            Instant createdAt) {
        return new Department(DepartmentId.generate(), name, costCenterCode, createdAt);
    }

    public DepartmentId id() {
        return id;
    }

    public DepartmentName name() {
        return name;
    }

    public CostCenterCode costCenterCode() {
        return costCenterCode;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
