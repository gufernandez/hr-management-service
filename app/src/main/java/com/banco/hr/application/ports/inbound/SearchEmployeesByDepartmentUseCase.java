package com.banco.hr.application.ports.inbound;

public interface SearchEmployeesByDepartmentUseCase {

    EmployeePage searchByDepartment(String departmentName, EmployeePageRequest pageRequest);
}
