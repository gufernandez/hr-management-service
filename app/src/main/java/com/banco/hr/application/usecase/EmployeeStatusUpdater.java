package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.EmployeeNotFoundException;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

final class EmployeeStatusUpdater {

    private final EmployeeRepositoryPort employeeRepository;
    private final Clock clock;

    EmployeeStatusUpdater(EmployeeRepositoryPort employeeRepository, Clock clock) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    Employee update(
            UUID employeeId,
            BiFunction<Employee, Instant, Employee> statusChange) {
        Objects.requireNonNull(employeeId, "O identificador do colaborador é obrigatório.");
        Objects.requireNonNull(statusChange);

        Employee employee = employeeRepository.findById(new EmployeeId(employeeId))
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        Employee updatedEmployee = statusChange.apply(employee, clock.instant());
        return Objects.requireNonNull(
                employeeRepository.save(updatedEmployee),
                "O repositório deve retornar o colaborador atualizado.");
    }
}
