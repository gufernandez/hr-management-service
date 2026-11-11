package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "department",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_department_name",
                columnNames = "name"))
public class DepartmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "cost_center_code", nullable = false, columnDefinition = "text")
    private String costCenterCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DepartmentJpaEntity() {
    }

    public DepartmentJpaEntity(
            UUID id,
            String name,
            String costCenterCode,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.costCenterCode = costCenterCode;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCostCenterCode() {
        return costCenterCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
