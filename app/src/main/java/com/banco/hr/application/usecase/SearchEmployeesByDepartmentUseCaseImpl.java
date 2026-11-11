package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.inbound.SearchEmployeesByDepartmentUseCase;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.DepartmentNameNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.domain.model.employee.Employee;

import java.util.List;
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
    public List<Employee> searchByDepartment(String departmentName) {
        DepartmentId departmentId = resolveDepartmentId(departmentName);
        return List.copyOf(Objects.requireNonNull(
                employeeRepository.findNotDeletedByDepartment(departmentId),
                "O repositório deve retornar a lista de colaboradores."));
    }

    @Override
    public EmployeePage searchByDepartment(
            String departmentName,
            EmployeePageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "A paginação é obrigatória.");
        DepartmentId departmentId = resolveDepartmentId(departmentName);

        return Objects.requireNonNull(
                employeeRepository.findNotDeletedByDepartment(departmentId, pageRequest),
                "O repositório deve retornar a página de colaboradores.");
    }

    private DepartmentId resolveDepartmentId(String departmentName) {
        DepartmentName name = new DepartmentName(departmentName);
        return departmentLookup.findIdByName(name)
                .orElseThrow(() -> new DepartmentNameNotFoundException(name.value()));
    }
}
