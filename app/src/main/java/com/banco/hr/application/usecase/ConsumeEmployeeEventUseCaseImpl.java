package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.ConsumeEmployeeEventUseCase;
import com.banco.hr.application.ports.inbound.EmployeeEventCommand;
import com.banco.hr.application.ports.outbound.EmployeeEventRepositoryPort;
import com.banco.hr.domain.exception.InvalidEmployeeEventException;
import com.banco.hr.domain.model.employee.EmployeeEvent;
import com.banco.hr.domain.model.employee.EmployeeEventType;

import java.util.Objects;

public class ConsumeEmployeeEventUseCaseImpl implements ConsumeEmployeeEventUseCase {

    private final EmployeeEventRepositoryPort employeeEventRepository;

    public ConsumeEmployeeEventUseCaseImpl(EmployeeEventRepositoryPort employeeEventRepository) {
        this.employeeEventRepository = Objects.requireNonNull(employeeEventRepository);
    }

    @Override
    public boolean consume(EmployeeEventCommand command) {
        Objects.requireNonNull(command, "O evento recebido é obrigatório.");
        Objects.requireNonNull(command.aggregateId(), "O aggregateId é obrigatório.");
        Objects.requireNonNull(command.employeeId(), "O employeeId é obrigatório.");
        if (!command.aggregateId().equals(command.employeeId())) {
            throw new InvalidEmployeeEventException(
                    "O aggregateId deve corresponder ao employeeId do payload.");
        }

        EmployeeEvent event = new EmployeeEvent(
                command.eventId(),
                command.employeeId(),
                command.eventTime(),
                mapEventType(command.eventName()),
                command.employeeStatus());
        if (employeeEventRepository.existsByEventId(command.eventId())) {
            return false;
        }

        employeeEventRepository.save(event);
        return true;
    }

    private EmployeeEventType mapEventType(String eventName) {
        return switch (Objects.requireNonNull(eventName, "O tipo do evento é obrigatório.")) {
            case "EmployeeCreatedEvent" -> EmployeeEventType.CREATION;
            case "EmployeeStatusChangedEvent" -> EmployeeEventType.UPDATE;
            default -> throw new InvalidEmployeeEventException(
                    "Tipo de evento de colaborador não suportado: " + eventName);
        };
    }
}
