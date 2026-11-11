package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.banco.hr.application.ports.outbound.PositionRepositoryPort;
import com.banco.hr.domain.exception.PositionAlreadyExistsException;
import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;
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
    public boolean existsByTitleAndLevel(PositionTitle title, PositionLevel level) {
        return positionJpaRepository.existsByTitleAndLevel(title.value(), level.value());
    }

    @Override
    public Position save(Position position) {
        try {
            var savedEntity = Objects.requireNonNull(
                    positionJpaRepository.save(positionPersistenceMapper.toEntity(position)),
                    "Saved position entity must not be null");
            return positionPersistenceMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            if (existsByTitleAndLevel(position.title(), position.level())) {
                throw new PositionAlreadyExistsException(
                        position.title().value(), position.level().value());
            }
            throw exception;
        }
    }
}
