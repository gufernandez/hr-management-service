package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.CreateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.dto.CreateEmployeeCommand;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.application.ports.outbound.PositionLookupPort;
import com.banco.hr.domain.exception.DepartmentNotFoundException;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.exception.PositionNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.position.PositionId;

import java.time.Clock;
import java.util.Objects;

public final class CreateEmployeeUseCaseImpl implements CreateEmployeeUseCase {

    private final EmployeeRepositoryPort employeeRepository;
    private final DepartmentLookupPort departmentLookup;
    private final PositionLookupPort positionLookup;
    private final Clock clock;

    public CreateEmployeeUseCaseImpl(
            EmployeeRepositoryPort employeeRepository,
            DepartmentLookupPort departmentLookup,
            PositionLookupPort positionLookup,
            Clock clock) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
        this.departmentLookup = Objects.requireNonNull(departmentLookup);
        this.positionLookup = Objects.requireNonNull(positionLookup);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Employee createEmployee(CreateEmployeeCommand command) {
        Objects.requireNonNull(command, "O comando de criação do colaborador é obrigatório.");

        EmployeeName name = new EmployeeName(command.name());
        Email email = new Email(command.email());
        DepartmentId departmentId = new DepartmentId(command.departmentId());
        PositionId positionId = new PositionId(command.positionId());

        if (!departmentLookup.existsById(departmentId)) {
            throw new DepartmentNotFoundException(departmentId.value());
        }
        if (!positionLookup.existsById(positionId)) {
            throw new PositionNotFoundException(positionId.value());
        }
        if (employeeRepository.existsByEmail(email)) {
            throw new EmployeeAlreadyExistsException(email.value());
        }

        Employee employee = Employee.create(
                name, email, departmentId, positionId, clock.instant());
        return Objects.requireNonNull(
                employeeRepository.save(employee),
                "O repositório deve retornar o colaborador salvo.");
    }
}
