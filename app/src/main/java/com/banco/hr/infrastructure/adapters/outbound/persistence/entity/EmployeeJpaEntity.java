package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "employee",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_employee_email",
                columnNames = "email"))
public class EmployeeJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "email", nullable = false, columnDefinition = "text")
    private String email;

    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    @Column(name = "position_id", nullable = false, updatable = false)
    private UUID positionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected EmployeeJpaEntity() {
    }

    public EmployeeJpaEntity(
            UUID id,
            String name,
            String email,
            UUID departmentId,
            UUID positionId,
            EmployeeStatus status,
            Instant createdAt,
            Instant lastUpdatedAt,
            Instant deletedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.status = status;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public UUID getPositionId() {
        return positionId;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
