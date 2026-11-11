package com.banco.hr.infrastructure.adapters.outbound.persistence.repository;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeeEventJpaRepository extends JpaRepository<EmployeeEventJpaEntity, UUID> {
}
