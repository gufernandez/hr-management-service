package com.banco.hr.application.ports.inbound;

import com.banco.hr.application.ports.inbound.dto.PageQuery;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeePage;

import java.util.List;

public interface ListEmployeesUseCase {

    List<Employee> listEmployees();

    EmployeePage listEmployees(PageQuery pageRequest);
}
