package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_status", nullable = false, length = 20)
    private EmployeeStatus employeeStatus;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private OutboxProcessingStatus processingStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected OutboxEventJpaEntity() {
    }

    public OutboxEventJpaEntity(
            UUID eventId,
            UUID employeeId,
            EmployeeStatus employeeStatus,
            Instant updateTime,
            String eventType,
            OutboxProcessingStatus processingStatus,
            Instant createdAt,
            Instant publishedAt,
            int retryCount) {
        this.eventId = eventId;
        this.employeeId = employeeId;
        this.employeeStatus = employeeStatus;
        this.updateTime = updateTime;
        this.eventType = eventType;
        this.processingStatus = processingStatus;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.retryCount = retryCount;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public EmployeeStatus getEmployeeStatus() {
        return employeeStatus;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public String getEventType() {
        return eventType;
    }

    public OutboxProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void markPublished(Instant publishedAt) {
        this.processingStatus = OutboxProcessingStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markFailed() {
        this.processingStatus = OutboxProcessingStatus.FAILED;
    }
}
