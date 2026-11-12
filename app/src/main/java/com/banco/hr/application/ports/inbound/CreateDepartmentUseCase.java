package com.banco.hr.application.ports.inbound;

import com.banco.hr.application.ports.inbound.dto.CreateDepartmentCommand;
import com.banco.hr.domain.model.department.Department;

public interface CreateDepartmentUseCase {

    Department createDepartment(CreateDepartmentCommand command);
}
