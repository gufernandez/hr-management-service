package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.DeactivateEmployeeUseCase;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.model.employee.Employee;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class DeactivateEmployeeUseCaseImpl implements DeactivateEmployeeUseCase {

    private final EmployeeStatusUpdater statusUpdater;

    public DeactivateEmployeeUseCaseImpl(EmployeeRepositoryPort employeeRepository, Clock clock) {
        statusUpdater = new EmployeeStatusUpdater(
                Objects.requireNonNull(employeeRepository), Objects.requireNonNull(clock));
    }

    @Override
    public Employee deactivateEmployee(UUID employeeId) {
        return statusUpdater.update(employeeId, Employee::deactivate);
    }
}
