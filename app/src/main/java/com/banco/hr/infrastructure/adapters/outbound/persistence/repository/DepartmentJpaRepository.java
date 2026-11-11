package com.banco.hr.infrastructure.adapters.outbound.persistence.repository;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentJpaEntity, UUID> {

    boolean existsByName(String name);
}
