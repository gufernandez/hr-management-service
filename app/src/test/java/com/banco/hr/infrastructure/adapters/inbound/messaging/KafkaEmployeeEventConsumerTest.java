package com.banco.hr.infrastructure.adapters.inbound.messaging;

import com.banco.hr.application.ports.inbound.ConsumeEmployeeEventUseCase;
import com.banco.hr.application.ports.inbound.EmployeeEventCommand;
import com.banco.hr.domain.exception.InvalidEmployeeEventException;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEmployeeEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("aa1f663d-6415-4121-bb14-94ba8e7977b0");
    private static final UUID EMPLOYEE_ID = UUID.fromString("5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e");
    private static final Instant EVENT_TIME = Instant.parse("2026-11-11T12:00:00Z");
    private static final String MESSAGE = """
            {
              "eventId": "aa1f663d-6415-4121-bb14-94ba8e7977b0",
              "eventType": "EmployeeStatusChangedEvent",
              "aggregateId": "5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e",
              "occurredAt": "2026-11-11T12:00:00Z",
              "payload": {
                "employeeId": "5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e",
                "status": "ACTIVE"
              }
            }
            """;

    @Mock
    private ConsumeEmployeeEventUseCase consumeEmployeeEventUseCase;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("Deve persistir o evento e confirmar Kafka após sucesso")
    void acknowledgesMessageAfterSuccessfulProcessing() {
        // Preparação: o caso de uso confirma que o evento foi salvo no histórico.
        when(consumeEmployeeEventUseCase.consume(any())).thenReturn(true);
        KafkaEmployeeEventConsumer consumer = consumer();

        // Execução: consome o registro e aguarda o retorno do caso de uso.
        consumer.consume(record(MESSAGE), acknowledgment);

        // Verificação: o comando contém os campos do envelope e o ACK ocorre após o processamento.
        ArgumentCaptor<EmployeeEventCommand> commandCaptor =
                ArgumentCaptor.forClass(EmployeeEventCommand.class);
        InOrder processingOrder = org.mockito.Mockito.inOrder(
                consumeEmployeeEventUseCase, acknowledgment);
        processingOrder.verify(consumeEmployeeEventUseCase).consume(commandCaptor.capture());
        EmployeeEventCommand command = commandCaptor.getValue();
        assertEquals(EVENT_ID, command.eventId());
        assertEquals("EmployeeStatusChangedEvent", command.eventName());
        assertEquals(EMPLOYEE_ID, command.aggregateId());
        assertEquals(EVENT_TIME, command.eventTime());
        assertEquals(EMPLOYEE_ID, command.employeeId());
        assertEquals(EmployeeStatus.ACTIVE, command.employeeStatus());
        processingOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Não deve confirmar Kafka quando a persistência falha")
    void doesNotAcknowledgeWhenProcessingFails() {
        // Preparação: a persistência falha e deve permitir que Kafka reentregue a mensagem.
        when(consumeEmployeeEventUseCase.consume(any()))
                .thenThrow(new IllegalStateException("Falha ao persistir histórico"));
        KafkaEmployeeEventConsumer consumer = consumer();

        // Execução e verificação: a falha é propagada sem confirmar o offset Kafka.
        assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(record(MESSAGE), acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Deve confirmar evento duplicado sem persistir uma segunda vez")
    void acknowledgesDuplicateAfterUseCaseConfirmsItWasAlreadyProcessed() {
        // Preparação: o caso de uso identifica o eventId como já persistido.
        when(consumeEmployeeEventUseCase.consume(any())).thenReturn(false);
        KafkaEmployeeEventConsumer consumer = consumer();

        // Execução: recebe novamente a mesma mensagem.
        consumer.consume(record(MESSAGE), acknowledgment);

        // Verificação: evento duplicado é confirmado somente após a verificação de idempotência.
        InOrder processingOrder = org.mockito.Mockito.inOrder(
                consumeEmployeeEventUseCase, acknowledgment);
        processingOrder.verify(consumeEmployeeEventUseCase).consume(any());
        processingOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Não deve confirmar mensagens que não podem ser interpretadas")
    void doesNotAcknowledgeInvalidMessage() {
        // Preparação: conteúdo inválido não representa um evento persistível.
        KafkaEmployeeEventConsumer consumer = consumer();

        // Execução e verificação: erro de formato é propagado sem ACK.
        assertThrows(
                InvalidEmployeeEventException.class,
                () -> consumer.consume(record("not-json"), acknowledgment));
        verify(consumeEmployeeEventUseCase, never()).consume(any());
        verify(acknowledgment, never()).acknowledge();
    }

    private KafkaEmployeeEventConsumer consumer() {
        return new KafkaEmployeeEventConsumer(
                new ObjectMapper().findAndRegisterModules(),
                consumeEmployeeEventUseCase);
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("hr.employee-events.v1", 0, 0L, EMPLOYEE_ID.toString(), value);
    }
}
