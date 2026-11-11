package com.banco.hr.infrastructure.config;

import com.banco.hr.application.ports.inbound.CreatePositionUseCase;
import com.banco.hr.application.ports.outbound.PositionRepositoryPort;
import com.banco.hr.application.usecase.CreatePositionUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PositionUseCaseConfiguration {

    @Bean
    public CreatePositionUseCase createPositionUseCase(PositionRepositoryPort positionRepository) {
        return new CreatePositionUseCaseImpl(positionRepository, Clock.systemUTC());
    }
}
