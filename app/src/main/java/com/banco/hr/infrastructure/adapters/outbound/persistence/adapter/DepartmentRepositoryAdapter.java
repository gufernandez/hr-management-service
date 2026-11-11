package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.application.ports.outbound.DepartmentRepositoryPort;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.DepartmentPersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.DepartmentJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentRepositoryAdapter implements DepartmentRepositoryPort {

    private final DepartmentJpaRepository departmentJpaRepository;
    private final DepartmentPersistenceMapper departmentPersistenceMapper;

    public DepartmentRepositoryAdapter(
            DepartmentJpaRepository departmentJpaRepository,
            DepartmentPersistenceMapper departmentPersistenceMapper) {
        this.departmentJpaRepository = departmentJpaRepository;
        this.departmentPersistenceMapper = departmentPersistenceMapper;
    }

    @Override
    public boolean existsByName(DepartmentName name) {
        return departmentJpaRepository.existsByName(name.value());
    }

    @Override
    public Department save(Department department) {
        var savedEntity = departmentJpaRepository.save(
                departmentPersistenceMapper.toEntity(department));
        return departmentPersistenceMapper.toDomain(savedEntity);
    }
}
