package com.banco.hr.infrastructure.adapters.outbound.messaging;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxProcessingStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class KafkaOutboxEventPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("aa1f663d-6415-4121-bb14-94ba8e7977b0");
    private static final UUID EMPLOYEE_ID = UUID.fromString("5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e");
    private static final Instant EVENT_TIME = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    @DisplayName("Deve enviar envelope e header KafkaEmployeeEvent ao tópico configurado")
    void publishesEventWithExpectedEnvelopeAndHeader() throws Exception {
        // Preparação: simula confirmação bem-sucedida do broker para o registro enviado.
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.completedFuture(
                        org.mockito.Mockito.mock(SendResult.class)));
        KafkaOutboxEventPublisher publisher =
                new KafkaOutboxEventPublisher(
                        kafkaTemplate,
                        new ObjectMapper().findAndRegisterModules(),
                        "hr.employee-events.v1");
        OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                EVENT_ID,
                EMPLOYEE_ID,
                EmployeeStatus.ACTIVE,
                EVENT_TIME,
                "EmployeeStatusChangedEvent",
                OutboxProcessingStatus.PENDING,
                EVENT_TIME,
                null,
                0);

        // Execução: publica a entrada persistida da outbox.
        publisher.publish(event);

        // Verificação: tópico, envelope e metadados do header seguem o contrato do RF-05.
        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();
        assertEquals("hr.employee-events.v1", record.topic());
        assertEquals(EMPLOYEE_ID.toString(), record.key());

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonNode payload = objectMapper.readTree(record.value());
        assertEquals(EVENT_ID.toString(), payload.get("eventId").asText());
        assertEquals("EmployeeStatusChangedEvent", payload.get("eventType").asText());
        assertEquals(EMPLOYEE_ID.toString(), payload.get("aggregateId").asText());
        assertEquals(EVENT_TIME.toString(), payload.get("occurredAt").asText());
        assertEquals(EMPLOYEE_ID.toString(), payload.at("/payload/employeeId").asText());
        assertEquals("ACTIVE", payload.at("/payload/status").asText());

        JsonNode header = objectMapper.readTree(
                record.headers().lastHeader("KafkaEmployeeEvent").value());
        assertEquals(EVENT_ID.toString(), header.get("eventId").asText());
        assertEquals(EMPLOYEE_ID.toString(), header.get("aggregateId").asText());
    }
}
