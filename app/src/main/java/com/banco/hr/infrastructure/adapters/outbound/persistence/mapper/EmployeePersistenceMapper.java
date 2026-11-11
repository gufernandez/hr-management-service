package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeePersistenceMapper {

    public EmployeeJpaEntity toEntity(Employee employee) {
        return new EmployeeJpaEntity(
                employee.id().value(),
                employee.name().value(),
                employee.email().value(),
                employee.departmentId().value(),
                employee.positionId().value(),
                employee.status(),
                employee.createdAt(),
                employee.lastUpdatedAt(),
                employee.deletedAt());
    }

    public Employee toDomain(EmployeeJpaEntity entity) {
        return Employee.reconstitute(
                new EmployeeId(entity.getId()),
                new EmployeeName(entity.getName()),
                new Email(entity.getEmail()),
                new DepartmentId(entity.getDepartmentId()),
                new PositionId(entity.getPositionId()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getLastUpdatedAt(),
                entity.getDeletedAt());
    }
}
