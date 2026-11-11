package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.position.Position;

public interface PositionRepositoryPort {

    Position save(Position position);
}
