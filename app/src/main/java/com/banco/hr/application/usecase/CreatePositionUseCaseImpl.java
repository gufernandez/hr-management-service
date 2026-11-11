package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.CreatePositionCommand;
import com.banco.hr.application.ports.inbound.CreatePositionUseCase;
import com.banco.hr.application.ports.outbound.PositionRepositoryPort;
import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;

import java.time.Clock;
import java.util.Objects;

public final class CreatePositionUseCaseImpl implements CreatePositionUseCase {

    private final PositionRepositoryPort positionRepository;
    private final Clock clock;

    public CreatePositionUseCaseImpl(PositionRepositoryPort positionRepository, Clock clock) {
        this.positionRepository = Objects.requireNonNull(positionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Position createPosition(CreatePositionCommand command) {
        Objects.requireNonNull(command, "O comando de criação do cargo é obrigatório.");

        PositionTitle title = new PositionTitle(command.title());
        PositionLevel level = new PositionLevel(command.level());

        Position position = Position.create(title, level, clock.instant());
        return Objects.requireNonNull(
                positionRepository.save(position),
                "O repositório deve retornar o cargo salvo.");
    }
}
