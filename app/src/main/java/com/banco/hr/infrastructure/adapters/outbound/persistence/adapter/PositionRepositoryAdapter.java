package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.banco.hr.application.ports.outbound.PositionRepositoryPort;
import com.banco.hr.domain.model.position.Position;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.PositionPersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.PositionJpaRepository;

@Repository
public class PositionRepositoryAdapter implements PositionRepositoryPort {

    private final PositionJpaRepository positionJpaRepository;
    private final PositionPersistenceMapper positionPersistenceMapper;

    public PositionRepositoryAdapter(
            PositionJpaRepository positionJpaRepository,
            PositionPersistenceMapper positionPersistenceMapper) {
        this.positionJpaRepository = positionJpaRepository;
        this.positionPersistenceMapper = positionPersistenceMapper;
    }

    @Override
    public Position save(Position position) {
        var savedEntity = Objects.requireNonNull(
                positionJpaRepository.save(positionPersistenceMapper.toEntity(position)),
                "Saved position entity must not be null");
        return positionPersistenceMapper.toDomain(savedEntity);
    }
}
