package com.banco.hr.infrastructure.adapters.outbound.persistence.repository;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE processing_status = 'PENDING'
            ORDER BY created_at, event_id
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingEventsForUpdate();
}
