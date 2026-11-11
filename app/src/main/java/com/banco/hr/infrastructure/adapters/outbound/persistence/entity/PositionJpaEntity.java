package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "position")
public class PositionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "level", nullable = false, columnDefinition = "text")
    private String level;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PositionJpaEntity() {
    }

    public PositionJpaEntity(UUID id, String title, String level, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
