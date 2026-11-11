package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentName;

public interface DepartmentRepositoryPort {

    boolean existsByName(DepartmentName name);

    Department save(Department department);
}
