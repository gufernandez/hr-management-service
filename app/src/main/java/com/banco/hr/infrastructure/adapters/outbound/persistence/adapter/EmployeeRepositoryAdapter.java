package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventType;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxProcessingStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.EmployeePersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeJpaRepository;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OutboxEventJpaRepository;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OffsetLimitPageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepositoryPort {

    private final EmployeeJpaRepository employeeJpaRepository;
    private final EmployeePersistenceMapper employeePersistenceMapper;
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public EmployeeRepositoryAdapter(
            EmployeeJpaRepository employeeJpaRepository,
            EmployeePersistenceMapper employeePersistenceMapper,
            OutboxEventJpaRepository outboxEventJpaRepository) {
        this.employeeJpaRepository = Objects.requireNonNull(employeeJpaRepository);
        this.employeePersistenceMapper = Objects.requireNonNull(employeePersistenceMapper);
        this.outboxEventJpaRepository = Objects.requireNonNull(outboxEventJpaRepository);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return employeeJpaRepository.existsByEmail(email.value());
    }

    @Override
    public Optional<Employee> findById(EmployeeId employeeId) {
        return employeeJpaRepository.findById(employeeId.value())
                .map(employeePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        Objects.requireNonNull(employee);
        EmployeeStatus previousStatus = employeeJpaRepository.findById(employee.id().value())
                .map(EmployeeJpaEntity::getStatus)
                .orElse(null);
        EmployeeJpaEntity savedEntity;
        try {
            savedEntity = Objects.requireNonNull(
                    employeeJpaRepository.save(employeePersistenceMapper.toEntity(employee)),
                    "O repositório JPA deve retornar o colaborador salvo.");
        } catch (DataIntegrityViolationException exception) {
            if (employeeJpaRepository.existsByEmail(employee.email().value())
                    && employeeJpaRepository.findById(employee.id().value()).isEmpty()) {
                throw new EmployeeAlreadyExistsException(employee.email().value());
            }
            throw exception;
        }
        if (previousStatus == null && employee.status() == EmployeeStatus.CREATED) {
            persistOutboxEvent(employee, OutboxEventType.EMPLOYEE_CREATED);
        } else if (previousStatus != null && previousStatus != employee.status()) {
            persistOutboxEvent(employee, OutboxEventType.EMPLOYEE_STATUS_CHANGED);
        }
        return employeePersistenceMapper.toDomain(savedEntity);
    }

    private void persistOutboxEvent(Employee employee, OutboxEventType eventType) {
        Instant eventTime = employee.lastUpdatedAt();
        outboxEventJpaRepository.save(new OutboxEventJpaEntity(
                UUID.randomUUID(),
                employee.id().value(),
                employee.status(),
                eventTime,
                eventType.value(),
                OutboxProcessingStatus.PENDING,
                eventTime,
                null,
                0));
    }

    @Override
    public List<Employee> findAllNotDeleted() {
        return employeeJpaRepository.findAllByStatusNotOrderByCreatedAtDesc(EmployeeStatus.DELETED)
                .stream()
                .map(employeePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public EmployeePage findAllNotDeleted(EmployeePageRequest pageRequest) {
        Page<EmployeeJpaEntity> page = employeeJpaRepository.findAllByStatusNot(
                EmployeeStatus.DELETED, pageable(pageRequest));
        return toEmployeePage(page, pageRequest);
    }

    @Override
    public EmployeePage findNotDeletedByDepartment(
            DepartmentId departmentId,
            EmployeePageRequest pageRequest) {
        Page<EmployeeJpaEntity> page = employeeJpaRepository.findAllByDepartmentIdAndStatusNot(
                departmentId.value(), EmployeeStatus.DELETED, pageable(pageRequest));
        return toEmployeePage(page, pageRequest);
    }

    @Override
    public List<Employee> findNotDeletedByDepartment(DepartmentId departmentId) {
        return employeeJpaRepository
                .findAllByDepartmentIdAndStatusNotOrderByCreatedAtDesc(
                        departmentId.value(), EmployeeStatus.DELETED)
                .stream()
                .map(employeePersistenceMapper::toDomain)
                .toList();
    }

    private OffsetLimitPageable pageable(EmployeePageRequest pageRequest) {
        return new OffsetLimitPageable(pageRequest.offset(), pageRequest.limit());
    }

    private EmployeePage toEmployeePage(
            Page<EmployeeJpaEntity> page,
            EmployeePageRequest pageRequest) {
        List<Employee> employees = page.getContent().stream()
                .map(employeePersistenceMapper::toDomain)
                .toList();
        return new EmployeePage(
                employees,
                pageRequest.offset(),
                pageRequest.limit(),
                page.getTotalElements());
    }
}
