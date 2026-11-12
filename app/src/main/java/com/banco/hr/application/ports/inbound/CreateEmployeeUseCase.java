package com.banco.hr.application.ports.inbound;

import com.banco.hr.application.ports.inbound.dto.CreateEmployeeCommand;
import com.banco.hr.domain.model.employee.Employee;

public interface CreateEmployeeUseCase {

    Employee createEmployee(CreateEmployeeCommand command);
}
