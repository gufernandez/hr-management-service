package com.banco.hr.infrastructure.adapters.inbound.rest.employee.dto;

import java.util.UUID;

import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.infrastructure.adapters.inbound.rest.ApiDateTimeFormatter;

public record EmployeeResponse(
        UUID id,
        String name,
        String email,
        UUID departmentId,
        UUID positionId,
        String status,
        String createdAt,
        String lastUpdatedAt,
        String deletedAt) {

    public static EmployeeResponse fromEmployee(Employee employee) {
        return new EmployeeResponse(
                employee.id().value(),
                employee.name().value(),
                employee.email().value(),
                employee.departmentId().value(),
                employee.positionId().value(),
                employee.status().name(),
                ApiDateTimeFormatter.format(employee.createdAt()),
                ApiDateTimeFormatter.format(employee.lastUpdatedAt()),
                employee.deletedAt() == null
                        ? null
                        : ApiDateTimeFormatter.format(employee.deletedAt()));
    }
}
