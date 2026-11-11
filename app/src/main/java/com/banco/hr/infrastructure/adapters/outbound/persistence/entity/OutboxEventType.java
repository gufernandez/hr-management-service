package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

public enum OutboxEventType {
    EMPLOYEE_CREATED("EmployeeCreatedEvent"),
    EMPLOYEE_STATUS_CHANGED("EmployeeStatusChangedEvent");

    private final String value;

    OutboxEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
