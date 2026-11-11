package com.banco.hr.infrastructure.adapters.inbound.messaging;

import com.banco.hr.application.ports.inbound.ConsumeEmployeeEventUseCase;
import com.banco.hr.application.ports.inbound.EmployeeEventCommand;
import com.banco.hr.domain.exception.InvalidEmployeeEventException;
import com.banco.hr.infrastructure.adapters.messaging.dto.KafkaEmployeeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KafkaEmployeeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEmployeeEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ConsumeEmployeeEventUseCase consumeEmployeeEventUseCase;

    public KafkaEmployeeEventConsumer(
            ObjectMapper objectMapper,
            ConsumeEmployeeEventUseCase consumeEmployeeEventUseCase) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.consumeEmployeeEventUseCase = Objects.requireNonNull(consumeEmployeeEventUseCase);
    }

    @KafkaListener(
            topics = "${outbox.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "employeeEventKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        KafkaEmployeeEvent event;
        try {
            event = objectMapper.readValue(record.value(), KafkaEmployeeEvent.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidEmployeeEventException(
                    "Não foi possível interpretar a mensagem Kafka de evento do colaborador.",
                    exception);
        }

        if (event == null || event.payload() == null) {
            throw new InvalidEmployeeEventException(
                    "A mensagem Kafka deve conter o payload do evento do colaborador.");
        }

        EmployeeEventCommand command = new EmployeeEventCommand(
                event.eventId(),
                event.eventType(),
                event.aggregateId(),
                event.occurredAt(),
                event.payload().employeeId(),
                event.payload().status());
        boolean stored = consumeEmployeeEventUseCase.consume(command);

        acknowledgment.acknowledge();
        if (stored) {
            log.info("Evento de colaborador persistido: eventId={}, employeeId={}",
                    event.eventId(), event.payload().employeeId());
        } else {
            log.info("Evento de colaborador duplicado confirmado: eventId={}", event.eventId());
        }
    }
}
