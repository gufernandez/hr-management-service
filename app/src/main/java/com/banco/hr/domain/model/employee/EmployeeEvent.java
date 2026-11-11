package com.banco.hr.domain.model.employee;

import com.banco.hr.domain.exception.InvalidEmployeeEventException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EmployeeEvent(
        UUID eventId,
        UUID employeeId,
        Instant eventTime,
        EmployeeEventType eventType,
        EmployeeStatus employeeStatus) {

    public EmployeeEvent {
        Objects.requireNonNull(eventId, "O identificador do evento é obrigatório.");
        Objects.requireNonNull(employeeId, "O identificador do colaborador é obrigatório.");
        Objects.requireNonNull(eventTime, "A data do evento é obrigatória.");
        Objects.requireNonNull(eventType, "O tipo do evento é obrigatório.");
        Objects.requireNonNull(employeeStatus, "O status do colaborador é obrigatório.");
        if (eventType == EmployeeEventType.CREATION && employeeStatus != EmployeeStatus.CREATED) {
            throw new InvalidEmployeeEventException("Evento CREATION deve possuir status CREATED.");
        }
        if (eventType == EmployeeEventType.UPDATE && employeeStatus == EmployeeStatus.CREATED) {
            throw new InvalidEmployeeEventException(
                    "Evento UPDATE não pode possuir status CREATED.");
        }
    }
}
