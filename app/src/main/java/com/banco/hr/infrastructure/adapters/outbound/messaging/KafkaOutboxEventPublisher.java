package com.banco.hr.infrastructure.adapters.outbound.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.banco.hr.infrastructure.adapters.messaging.dto.KafkaEmployeeEvent;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KafkaOutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaOutboxEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${outbox.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(OutboxEventJpaEntity outboxEvent) {
        KafkaEmployeeEvent event = new KafkaEmployeeEvent(
                outboxEvent.getEventId(),
                outboxEvent.getEventType(),
                outboxEvent.getEmployeeId(),
                outboxEvent.getUpdateTime(),
                new KafkaEmployeeEvent.Payload(
                        outboxEvent.getEmployeeId(),
                        outboxEvent.getEmployeeStatus()));

        try {
            String payload = objectMapper.writeValueAsString(event);
            String header = objectMapper.writeValueAsString(new EventHeader(
                    outboxEvent.getEventId(),
                    outboxEvent.getEmployeeId()));
            RecordHeaders headers = new RecordHeaders();
            headers.add("KafkaEmployeeEvent", header.getBytes(StandardCharsets.UTF_8));
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    null,
                    outboxEvent.getEmployeeId().toString(),
                    payload,
                    headers);
            kafkaTemplate.send(record).get();
        } catch (JsonProcessingException | ExecutionException | KafkaException exception) {
            throw new KafkaPublishException(
                    "Falha ao publicar evento de outbox " + outboxEvent.getEventId(),
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException(
                    "Publicação do evento de outbox interrompida " + outboxEvent.getEventId(),
                    exception);
        }
    }

    private record EventHeader(UUID eventId, UUID aggregateId) {
    }
}
