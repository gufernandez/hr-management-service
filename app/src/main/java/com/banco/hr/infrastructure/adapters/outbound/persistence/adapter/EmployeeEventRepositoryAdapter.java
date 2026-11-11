package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.banco.hr.application.ports.outbound.EmployeeEventRepositoryPort;
import com.banco.hr.domain.model.employee.EmployeeEvent;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeEventJpaRepository;

@Repository
public class EmployeeEventRepositoryAdapter implements EmployeeEventRepositoryPort {

    private final EmployeeEventJpaRepository employeeEventJpaRepository;

    public EmployeeEventRepositoryAdapter(EmployeeEventJpaRepository employeeEventJpaRepository) {
        this.employeeEventJpaRepository = employeeEventJpaRepository;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return employeeEventJpaRepository.existsById(eventId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(EmployeeEvent employeeEvent) {
        employeeEventJpaRepository.saveAndFlush(EmployeeEventJpaEntity.fromDomain(employeeEvent));
    }
}
