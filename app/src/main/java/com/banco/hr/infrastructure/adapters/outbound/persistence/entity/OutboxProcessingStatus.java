package com.banco.hr.infrastructure.adapters.outbound.persistence.entity;

public enum OutboxProcessingStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
