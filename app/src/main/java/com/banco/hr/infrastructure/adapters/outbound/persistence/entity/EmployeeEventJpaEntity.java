package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

import com.banco.hr.domain.model.employee.EmployeeEvent;
import com.banco.hr.domain.model.employee.EmployeeEventType;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_events")
public class EmployeeEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "event_time", nullable = false, updatable = false)
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20, updatable = false)
    private EmployeeEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_status", nullable = false, length = 20, updatable = false)
    private EmployeeStatus employeeStatus;

    protected EmployeeEventJpaEntity() {
    }

    public EmployeeEventJpaEntity(
            UUID eventId,
            UUID employeeId,
            Instant eventTime,
            EmployeeEventType eventType,
            EmployeeStatus employeeStatus) {
        this.eventId = eventId;
        this.employeeId = employeeId;
        this.eventTime = eventTime;
        this.eventType = eventType;
        this.employeeStatus = employeeStatus;
    }

    public static EmployeeEventJpaEntity fromDomain(EmployeeEvent event) {
        return new EmployeeEventJpaEntity(
                event.eventId(),
                event.employeeId(),
                event.eventTime(),
                event.eventType(),
                event.employeeStatus());
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public EmployeeEventType getEventType() {
        return eventType;
    }

    public EmployeeStatus getEmployeeStatus() {
        return employeeStatus;
    }
}
