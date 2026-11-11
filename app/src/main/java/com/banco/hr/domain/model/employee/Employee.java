package com.banco.hr.domain.model.employee;

import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.position.PositionId;

import java.time.Instant;
import java.util.Objects;

public final class Employee {

    private final EmployeeId id;
    private final EmployeeName name;
    private final Email email;
    private final DepartmentId departmentId;
    private final PositionId positionId;
    private final EmployeeStatus status;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;
    private final Instant deletedAt;

    private Employee(
            EmployeeId id,
            EmployeeName name,
            Email email,
            DepartmentId departmentId,
            PositionId positionId,
            EmployeeStatus status,
            Instant createdAt,
            Instant lastUpdatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "O identificador do colaborador é obrigatório.");
        this.name = Objects.requireNonNull(name, "O nome do colaborador é obrigatório.");
        this.email = Objects.requireNonNull(email, "O e-mail do colaborador é obrigatório.");
        this.departmentId = Objects.requireNonNull(
                departmentId, "O departamento do colaborador é obrigatório.");
        this.positionId = Objects.requireNonNull(
                positionId, "O cargo do colaborador é obrigatório.");
        this.status = Objects.requireNonNull(status, "O status do colaborador é obrigatório.");
        this.createdAt = Objects.requireNonNull(
                createdAt, "A data de criação do colaborador é obrigatória.");
        this.lastUpdatedAt = Objects.requireNonNull(
                lastUpdatedAt, "A data de atualização do colaborador é obrigatória.");
        if (lastUpdatedAt.isBefore(createdAt)) {
            throw new DomainException(
                    "A data de atualização não pode ser anterior à data de criação.");
        }
        if (status == EmployeeStatus.DELETED) {
            this.deletedAt = Objects.requireNonNull(
                    deletedAt, "A data de exclusão do colaborador é obrigatória.");
            if (deletedAt.isBefore(createdAt) || deletedAt.isAfter(lastUpdatedAt)) {
                throw new DomainException(
                        "A data de exclusão deve estar entre a criação e a última atualização.");
            }
        } else {
            if (deletedAt != null) {
                throw new DomainException(
                        "A data de exclusão só pode ser informada para colaboradores excluídos.");
            }
            this.deletedAt = null;
        }
    }

    public static Employee create(
            EmployeeName name,
            Email email,
            DepartmentId departmentId,
            PositionId positionId,
            Instant createdAt) {
        return new Employee(
                EmployeeId.generate(),
                name,
                email,
                departmentId,
                positionId,
                EmployeeStatus.CREATED,
                createdAt,
                createdAt,
                null);
    }

    public static Employee reconstitute(
            EmployeeId id,
            EmployeeName name,
            Email email,
            DepartmentId departmentId,
            PositionId positionId,
            EmployeeStatus status,
            Instant createdAt,
            Instant lastUpdatedAt,
            Instant deletedAt) {
        return new Employee(
                id,
                name,
                email,
                departmentId,
                positionId,
                status,
                createdAt,
                lastUpdatedAt,
                deletedAt);
    }

    public Employee activate(Instant updatedAt) {
        if (status != EmployeeStatus.CREATED && status != EmployeeStatus.INACTIVE) {
            throw new DomainException(
                    "Só é possível ativar colaboradores com status CREATED ou INACTIVE.");
        }
        return withStatus(EmployeeStatus.ACTIVE, updatedAt, null);
    }

    public Employee deactivate(Instant updatedAt) {
        if (status != EmployeeStatus.CREATED && status != EmployeeStatus.ACTIVE) {
            throw new DomainException(
                    "Só é possível desativar colaboradores com status CREATED ou ACTIVE.");
        }
        return withStatus(EmployeeStatus.INACTIVE, updatedAt, null);
    }

    public Employee delete(Instant deletedAt) {
        if (status == EmployeeStatus.DELETED) {
            throw new DomainException("O colaborador já está excluído.");
        }
        return withStatus(EmployeeStatus.DELETED, deletedAt, deletedAt);
    }

    private Employee withStatus(
            EmployeeStatus newStatus,
            Instant updatedAt,
            Instant newDeletedAt) {
        Objects.requireNonNull(updatedAt, "A data de atualização é obrigatória.");
        if (updatedAt.isBefore(lastUpdatedAt)) {
            throw new DomainException(
                    "A data de atualização não pode ser anterior à última atualização.");
        }
        return new Employee(
                id,
                name,
                email,
                departmentId,
                positionId,
                newStatus,
                createdAt,
                updatedAt,
                newDeletedAt);
    }

    public EmployeeId id() {
        return id;
    }

    public EmployeeName name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public DepartmentId departmentId() {
        return departmentId;
    }

    public PositionId positionId() {
        return positionId;
    }

    public EmployeeStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
