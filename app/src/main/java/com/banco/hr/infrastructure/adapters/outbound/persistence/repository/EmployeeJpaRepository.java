package com.banco.hr.infrastructure.adapters.outbound.persistence.repository;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    boolean existsByEmail(String email);

    List<EmployeeJpaEntity> findAllByStatusNotOrderByCreatedAtDesc(EmployeeStatus status);

    Page<EmployeeJpaEntity> findAllByStatusNot(
            EmployeeStatus status,
            Pageable pageable);

    Page<EmployeeJpaEntity> findAllByDepartmentIdAndStatusNot(
            UUID departmentId,
            EmployeeStatus status,
            Pageable pageable);

    List<EmployeeJpaEntity> findAllByDepartmentIdAndStatusNotOrderByCreatedAtDesc(
            UUID departmentId,
            EmployeeStatus status);
}
