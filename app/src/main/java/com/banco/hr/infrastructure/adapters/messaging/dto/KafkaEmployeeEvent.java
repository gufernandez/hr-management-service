package com.banco.hr.infrastructure.adapters.messaging.dto;

import java.time.Instant;
import java.util.UUID;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

public record KafkaEmployeeEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt,
        Payload payload) {

    public record Payload(UUID employeeId, EmployeeStatus status) {
    }
}
