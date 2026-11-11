package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.EmployeeEventCommand;
import com.banco.hr.application.ports.outbound.EmployeeEventRepositoryPort;
import com.banco.hr.domain.exception.InvalidEmployeeEventException;
import com.banco.hr.domain.model.employee.EmployeeEvent;
import com.banco.hr.domain.model.employee.EmployeeEventType;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeEmployeeEventUseCaseImplTest {

    private static final UUID EVENT_ID = UUID.fromString("aa1f663d-6415-4121-bb14-94ba8e7977b0");
    private static final UUID EMPLOYEE_ID = UUID.fromString("5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e");
    private static final Instant EVENT_TIME = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private EmployeeEventRepositoryPort employeeEventRepository;

    @Test
    @DisplayName("Deve persistir EmployeeCreatedEvent como evento de criação")
    void persistsCreationEvent() {
        // Preparação: evento de criação ainda não foi processado.
        when(employeeEventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);

        // Execução: converte e processa o evento recebido do Kafka.
        boolean processed = useCase.consume(command(
                "EmployeeCreatedEvent", EmployeeStatus.CREATED));

        // Verificação: o histórico armazena CREATION e o status CREATED.
        assertTrue(processed);
        ArgumentCaptor<EmployeeEvent> eventCaptor = ArgumentCaptor.forClass(EmployeeEvent.class);
        verify(employeeEventRepository).save(eventCaptor.capture());
        EmployeeEvent savedEvent = eventCaptor.getValue();
        assertEquals(EVENT_ID, savedEvent.eventId());
        assertEquals(EMPLOYEE_ID, savedEvent.employeeId());
        assertEquals(EVENT_TIME, savedEvent.eventTime());
        assertEquals(EmployeeEventType.CREATION, savedEvent.eventType());
        assertEquals(EmployeeStatus.CREATED, savedEvent.employeeStatus());
    }

    @ParameterizedTest
    @EnumSource(value = EmployeeStatus.class, names = {"ACTIVE", "INACTIVE", "DELETED"})
    @DisplayName("Deve persistir mudanças de status como eventos de atualização")
    void persistsStatusChangeAsUpdate(EmployeeStatus status) {
        // Preparação: evento de mudança de status ainda não foi processado.
        when(employeeEventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);

        // Execução: converte o evento de status recebido do Kafka.
        boolean processed = useCase.consume(command("EmployeeStatusChangedEvent", status));

        // Verificação: o histórico classifica o evento como UPDATE e mantém o status correto.
        assertTrue(processed);
        ArgumentCaptor<EmployeeEvent> eventCaptor = ArgumentCaptor.forClass(EmployeeEvent.class);
        verify(employeeEventRepository).save(eventCaptor.capture());
        assertEquals(EmployeeEventType.UPDATE, eventCaptor.getValue().eventType());
        assertEquals(status, eventCaptor.getValue().employeeStatus());
    }

    @Test
    @DisplayName("Não deve persistir novamente um eventId já processado")
    void ignoresDuplicateEventId() {
        // Preparação: o eventId já existe no histórico.
        when(employeeEventRepository.existsByEventId(EVENT_ID)).thenReturn(true);
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);

        // Execução: o mesmo evento é recebido novamente após uma entrega repetida.
        boolean processed = useCase.consume(command(
                "EmployeeCreatedEvent", EmployeeStatus.CREATED));

        // Verificação: informa duplicidade para o consumer confirmar sem gravar outra linha.
        assertFalse(processed);
        verify(employeeEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Deve rejeitar evento de criação com status diferente de CREATED")
    void rejectsCreationEventWithInvalidStatus() {
        // Preparação: o tipo de evento e o status violam o contrato do RF-06.
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);

        // Execução e verificação: evento inválido não deve ser persistido.
        assertThrows(InvalidEmployeeEventException.class, () -> useCase.consume(
                command("EmployeeCreatedEvent", EmployeeStatus.ACTIVE)));
        verify(employeeEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Deve rejeitar evento de atualização com status CREATED")
    void rejectsStatusChangedEventWithCreatedStatus() {
        // Preparação: CREATED só pode ser registrado pelo evento de criação.
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);

        // Execução e verificação: evento incompatível não deve consultar nem salvar histórico.
        assertThrows(InvalidEmployeeEventException.class, () -> useCase.consume(
                command("EmployeeStatusChangedEvent", EmployeeStatus.CREATED)));
        verify(employeeEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Deve rejeitar evento quando aggregateId diverge do employeeId")
    void rejectsEventWithMismatchedAggregateId() {
        // Preparação: o envelope e o payload identificam agregados diferentes.
        ConsumeEmployeeEventUseCaseImpl useCase =
                new ConsumeEmployeeEventUseCaseImpl(employeeEventRepository);
        EmployeeEventCommand command = new EmployeeEventCommand(
                EVENT_ID,
                "EmployeeCreatedEvent",
                UUID.randomUUID(),
                EVENT_TIME,
                EMPLOYEE_ID,
                EmployeeStatus.CREATED);

        // Execução e verificação: inconsistência não deve resultar em persistência.
        assertThrows(InvalidEmployeeEventException.class, () -> useCase.consume(command));
        verify(employeeEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private EmployeeEventCommand command(String eventName, EmployeeStatus status) {
        return new EmployeeEventCommand(
                EVENT_ID, eventName, EMPLOYEE_ID, EVENT_TIME, EMPLOYEE_ID, status);
    }
}
