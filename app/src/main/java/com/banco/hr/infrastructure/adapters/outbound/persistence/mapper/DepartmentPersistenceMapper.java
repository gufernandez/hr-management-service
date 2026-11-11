package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.department.CostCenterCode;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentPersistenceMapper {

    public DepartmentJpaEntity toEntity(Department department) {
        return new DepartmentJpaEntity(
                department.id().value(),
                department.name().value(),
                department.costCenterCode().value(),
                department.createdAt());
    }

    public Department toDomain(DepartmentJpaEntity entity) {
        return Department.reconstitute(
                new DepartmentId(entity.getId()),
                new DepartmentName(entity.getName()),
                new CostCenterCode(entity.getCostCenterCode()),
                entity.getCreatedAt());
    }
}
