package com.banco.hr.infrastructure.config;

import com.banco.hr.application.ports.inbound.CreateDepartmentUseCase;
import com.banco.hr.application.ports.outbound.DepartmentRepositoryPort;
import com.banco.hr.application.usecase.CreateDepartmentUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DepartmentUseCaseConfiguration {

    @Bean
    public CreateDepartmentUseCase createDepartmentUseCase(
            DepartmentRepositoryPort departmentRepository) {
        return new CreateDepartmentUseCaseImpl(departmentRepository, Clock.systemUTC());
    }
}
