package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PositionPersistenceMapper {

    public PositionJpaEntity toEntity(Position position) {
        return new PositionJpaEntity(
                position.id().value(),
                position.title().value(),
                position.level().value(),
                position.createdAt());
    }

    public Position toDomain(PositionJpaEntity entity) {
        return Position.reconstitute(
                new PositionId(entity.getId()),
                new PositionTitle(entity.getTitle()),
                new PositionLevel(entity.getLevel()),
                entity.getCreatedAt());
    }
}
