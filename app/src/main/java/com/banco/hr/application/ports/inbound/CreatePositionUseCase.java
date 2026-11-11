package com.banco.hr.application.ports.inbound;

import com.banco.hr.domain.model.position.Position;

public interface CreatePositionUseCase {

    Position createPosition(CreatePositionCommand command);
}
