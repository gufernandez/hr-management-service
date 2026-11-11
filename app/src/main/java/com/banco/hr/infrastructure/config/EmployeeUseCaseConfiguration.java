package com.banco.hr.infrastructure.config;

import com.banco.hr.application.ports.inbound.ActivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.CreateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeactivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeleteEmployeeUseCase;
import com.banco.hr.application.ports.inbound.ListEmployeesUseCase;
import com.banco.hr.application.ports.inbound.SearchEmployeesByDepartmentUseCase;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.application.ports.outbound.PositionLookupPort;
import com.banco.hr.application.usecase.ActivateEmployeeUseCaseImpl;
import com.banco.hr.application.usecase.CreateEmployeeUseCaseImpl;
import com.banco.hr.application.usecase.DeactivateEmployeeUseCaseImpl;
import com.banco.hr.application.usecase.DeleteEmployeeUseCaseImpl;
import com.banco.hr.application.usecase.ListEmployeesUseCaseImpl;
import com.banco.hr.application.usecase.SearchEmployeesByDepartmentUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class EmployeeUseCaseConfiguration {

    @Bean
    public CreateEmployeeUseCase createEmployeeUseCase(
            EmployeeRepositoryPort employeeRepository,
            DepartmentLookupPort departmentLookup,
            PositionLookupPort positionLookup) {
        return new CreateEmployeeUseCaseImpl(
                employeeRepository, departmentLookup, positionLookup, Clock.systemUTC());
    }

    @Bean
    public ListEmployeesUseCase listEmployeesUseCase(EmployeeRepositoryPort employeeRepository) {
        return new ListEmployeesUseCaseImpl(employeeRepository);
    }

    @Bean
    public DeleteEmployeeUseCase deleteEmployeeUseCase(EmployeeRepositoryPort employeeRepository) {
        return new DeleteEmployeeUseCaseImpl(employeeRepository, Clock.systemUTC());
    }

    @Bean
    public SearchEmployeesByDepartmentUseCase searchEmployeesByDepartmentUseCase(
            DepartmentLookupPort departmentLookup,
            EmployeeRepositoryPort employeeRepository) {
        return new SearchEmployeesByDepartmentUseCaseImpl(departmentLookup, employeeRepository);
    }

    @Bean
    public ActivateEmployeeUseCase activateEmployeeUseCase(
            EmployeeRepositoryPort employeeRepository) {
        return new ActivateEmployeeUseCaseImpl(employeeRepository, Clock.systemUTC());
    }

    @Bean
    public DeactivateEmployeeUseCase deactivateEmployeeUseCase(
            EmployeeRepositoryPort employeeRepository) {
        return new DeactivateEmployeeUseCaseImpl(employeeRepository, Clock.systemUTC());
    }
}
