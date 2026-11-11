package com.banco.hr.infrastructure.adapters.inbound.rest.department;

import com.banco.hr.domain.model.department.Department;
import com.banco.hr.infrastructure.adapters.inbound.rest.ApiDateTimeFormatter;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String costCenterCode,
        String createdAt) {

    public static DepartmentResponse fromDepartment(Department department) {
        return new DepartmentResponse(
                department.id().value(),
                department.name().value(),
                department.costCenterCode().value(),
                ApiDateTimeFormatter.format(department.createdAt()));
    }
}
