package com.banco.hr.application.ports.outbound;

import com.banco.hr.domain.model.position.PositionId;

public interface PositionLookupPort {

    boolean existsById(PositionId positionId);
}
