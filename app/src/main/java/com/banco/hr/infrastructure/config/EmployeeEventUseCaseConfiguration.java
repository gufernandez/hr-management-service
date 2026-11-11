package com.banco.hr.infrastructure.config;

import com.banco.hr.application.ports.inbound.ConsumeEmployeeEventUseCase;
import com.banco.hr.application.ports.outbound.EmployeeEventRepositoryPort;
import com.banco.hr.application.usecase.ConsumeEmployeeEventUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeEventUseCaseConfiguration {

    @Bean
    public ConsumeEmployeeEventUseCase consumeEmployeeEventUseCase(
            EmployeeEventRepositoryPort employeeEventRepository) {
        return new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);
    }
}
