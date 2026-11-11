package com.banco.hr.application.ports.outbound;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepositoryPort {

    boolean existsByEmail(Email email);

    Optional<Employee> findById(EmployeeId employeeId);

    Employee save(Employee employee);

    List<Employee> findAllNotDeleted();

    EmployeePage findAllNotDeleted(EmployeePageRequest pageRequest);

    EmployeePage findNotDeletedByDepartment(
            DepartmentId departmentId,
            EmployeePageRequest pageRequest);
}
