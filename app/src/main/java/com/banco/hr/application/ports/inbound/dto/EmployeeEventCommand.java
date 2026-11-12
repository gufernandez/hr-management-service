package com.banco.hr.application.ports.inbound.dto;

import com.banco.hr.domain.model.employee.EmployeeStatus;

import java.time.Instant;
import java.util.UUID;

public record EmployeeEventCommand(
        UUID eventId,
        String eventName,
        UUID aggregateId,
        Instant eventTime,
        UUID employeeId,
        EmployeeStatus employeeStatus) {
}
