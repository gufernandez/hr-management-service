package com.banco.hr.application.ports.inbound;

import com.banco.hr.domain.model.employee.Employee;

import java.util.List;

public interface SearchEmployeesByDepartmentUseCase {

    List<Employee> searchByDepartment(String departmentName);

    EmployeePage searchByDepartment(String departmentName, EmployeePageRequest pageRequest);
}
