package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;

import java.util.Optional;

public interface DepartmentLookupPort {

    boolean existsById(DepartmentId departmentId);

    Optional<DepartmentId> findIdByName(DepartmentName departmentName);
}
