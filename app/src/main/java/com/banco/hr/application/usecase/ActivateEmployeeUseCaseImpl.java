package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.ActivateEmployeeUseCase;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.model.employee.Employee;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class ActivateEmployeeUseCaseImpl implements ActivateEmployeeUseCase {

    private final EmployeeStatusUpdater statusUpdater;

    public ActivateEmployeeUseCaseImpl(EmployeeRepositoryPort employeeRepository, Clock clock) {
        statusUpdater = new EmployeeStatusUpdater(
                Objects.requireNonNull(employeeRepository), Objects.requireNonNull(clock));
    }

    @Override
    public Employee activateEmployee(UUID employeeId) {
        return statusUpdater.update(employeeId, Employee::activate);
    }
}
