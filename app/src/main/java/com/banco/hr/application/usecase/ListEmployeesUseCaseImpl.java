package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.inbound.ListEmployeesUseCase;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.model.employee.Employee;

import java.util.List;
import java.util.Objects;

public final class ListEmployeesUseCaseImpl implements ListEmployeesUseCase {

    private final EmployeeRepositoryPort employeeRepository;

    public ListEmployeesUseCaseImpl(EmployeeRepositoryPort employeeRepository) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
    }

    @Override
    public List<Employee> listEmployees() {
        return List.copyOf(Objects.requireNonNull(
                employeeRepository.findAllNotDeleted(),
                "O repositório deve retornar a lista de colaboradores."));
    }

    @Override
    public EmployeePage listEmployees(EmployeePageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "A paginação é obrigatória.");
        return Objects.requireNonNull(
                employeeRepository.findAllNotDeleted(pageRequest),
                "O repositório deve retornar a página de colaboradores.");
    }
}
