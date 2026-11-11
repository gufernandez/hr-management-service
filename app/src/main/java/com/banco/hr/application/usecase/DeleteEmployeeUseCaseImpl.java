package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.DeleteEmployeeUseCase;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.model.employee.Employee;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class DeleteEmployeeUseCaseImpl implements DeleteEmployeeUseCase {

    private final EmployeeStatusUpdater statusUpdater;

    public DeleteEmployeeUseCaseImpl(EmployeeRepositoryPort employeeRepository, Clock clock) {
        statusUpdater = new EmployeeStatusUpdater(
                Objects.requireNonNull(employeeRepository), Objects.requireNonNull(clock));
    }

    @Override
    public Employee deleteEmployee(UUID employeeId) {
        return statusUpdater.update(employeeId, Employee::delete);
    }
}
