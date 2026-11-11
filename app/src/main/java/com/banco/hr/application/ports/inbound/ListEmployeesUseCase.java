package com.banco.hr.application.ports.inbound;

import com.banco.hr.domain.model.employee.Employee;

import java.util.List;

public interface ListEmployeesUseCase {

    List<Employee> listEmployees();

    EmployeePage listEmployees(EmployeePageRequest pageRequest);
}
