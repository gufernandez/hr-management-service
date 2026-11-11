package com.banco.hr.infrastructure.adapters.outbound.persistence.repository;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionJpaRepository extends JpaRepository<PositionJpaEntity, UUID> {
}
