package com.banco.hr.application.ports.inbound;

import com.banco.hr.domain.model.employee.Employee;

import java.util.UUID;

public interface DeactivateEmployeeUseCase {

    Employee deactivateEmployee(UUID employeeId);
}
