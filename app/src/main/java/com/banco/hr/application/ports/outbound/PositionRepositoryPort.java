package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;

public interface PositionRepositoryPort {

    boolean existsByTitleAndLevel(PositionTitle title, PositionLevel level);

    Position save(Position position);
}
