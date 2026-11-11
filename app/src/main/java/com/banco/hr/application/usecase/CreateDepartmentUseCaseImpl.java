package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.CreateDepartmentCommand;
import com.banco.hr.application.ports.inbound.CreateDepartmentUseCase;
import com.banco.hr.application.ports.outbound.DepartmentRepositoryPort;
import com.banco.hr.domain.exception.DepartmentAlreadyExistsException;
import com.banco.hr.domain.model.department.CostCenterCode;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentName;

import java.time.Clock;
import java.util.Objects;

public final class CreateDepartmentUseCaseImpl implements CreateDepartmentUseCase {

    private final DepartmentRepositoryPort departmentRepository;
    private final Clock clock;

    public CreateDepartmentUseCaseImpl(DepartmentRepositoryPort departmentRepository, Clock clock) {
        this.departmentRepository = Objects.requireNonNull(departmentRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Department createDepartment(CreateDepartmentCommand command) {
        Objects.requireNonNull(command, "O comando de criação do departamento é obrigatório.");

        DepartmentName name = new DepartmentName(command.name());
        CostCenterCode costCenterCode = new CostCenterCode(command.costCenterCode());

        if (departmentRepository.existsByName(name)) {
            throw new DepartmentAlreadyExistsException(name.value());
        }

        Department department = Department.create(name, costCenterCode, clock.instant());
        return Objects.requireNonNull(
                departmentRepository.save(department),
                "O repositório deve retornar o departamento salvo.");
    }
}
