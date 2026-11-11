package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.inbound.SearchEmployeesByDepartmentUseCase;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.DepartmentNameNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;

import java.util.Objects;

public final class SearchEmployeesByDepartmentUseCaseImpl
        implements SearchEmployeesByDepartmentUseCase {

    private final DepartmentLookupPort departmentLookup;
    private final EmployeeRepositoryPort employeeRepository;

    public SearchEmployeesByDepartmentUseCaseImpl(
            DepartmentLookupPort departmentLookup,
            EmployeeRepositoryPort employeeRepository) {
        this.departmentLookup = Objects.requireNonNull(departmentLookup);
        this.employeeRepository = Objects.requireNonNull(employeeRepository);
    }

    @Override
    public EmployeePage searchByDepartment(
            String departmentName,
            EmployeePageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "A paginação é obrigatória.");
        DepartmentName name = new DepartmentName(departmentName);
        DepartmentId departmentId = departmentLookup.findIdByName(name)
                .orElseThrow(() -> new DepartmentNameNotFoundException(name.value()));

        return Objects.requireNonNull(
                employeeRepository.findNotDeletedByDepartment(departmentId, pageRequest),
                "O repositório deve retornar a página de colaboradores.");
    }
}
