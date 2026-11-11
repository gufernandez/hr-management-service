package com.banco.hr.infrastructure.adapters.outbound.messaging;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxProcessingStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    private static final UUID EVENT_ID = UUID.fromString("aa1f663d-6415-4121-bb14-94ba8e7977b0");
    private static final UUID EMPLOYEE_ID = UUID.fromString("5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e");
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private OutboxEventJpaRepository outboxEventRepository;

    @Mock
    private KafkaOutboxEventPublisher eventPublisher;

    @Test
    @DisplayName("Deve publicar eventos pendentes e marcar como publicados após ACK")
    void publishesPendingEventAndMarksItPublished() {
        // Preparação: disponibiliza um evento pendente e um relógio determinístico.
        OutboxEventJpaEntity event = event(0);
        when(outboxEventRepository.findPendingEventsForUpdate()).thenReturn(List.of(event));
        OutboxPoller poller = poller(5);

        // Execução: processa os eventos pendentes da fila de outbox.
        poller.publishPendingEvents();

        // Verificação: o envio ocorreu e o ACK marcou o evento com timestamp de publicação.
        verify(eventPublisher).publish(event);
        assertEquals(OutboxProcessingStatus.PUBLISHED, event.getProcessingStatus());
        assertEquals(NOW, event.getPublishedAt());
    }

    @Test
    @DisplayName("Deve incrementar retry e manter pendente enquanto houver tentativas")
    void incrementsRetryAndKeepsEventPendingBeforeRetryLimit() {
        // Preparação: simula falha de envio antes de atingir o limite configurado.
        OutboxEventJpaEntity event = event(1);
        when(outboxEventRepository.findPendingEventsForUpdate()).thenReturn(List.of(event));
        doThrow(new KafkaPublishException("Falha Kafka", new RuntimeException()))
                .when(eventPublisher).publish(event);
        OutboxPoller poller = poller(3);

        // Execução: processa o evento cuja publicação falhou.
        poller.publishPendingEvents();

        // Verificação: a falha é registrada sem impedir nova tentativa futura.
        assertEquals(2, event.getRetryCount());
        assertEquals(OutboxProcessingStatus.PENDING, event.getProcessingStatus());
    }

    @Test
    @DisplayName("Deve marcar evento como falho ao atingir o máximo de retries")
    void marksEventFailedWhenRetryLimitIsReached() {
        // Preparação: a próxima falha alcançará o limite máximo de tentativas.
        OutboxEventJpaEntity event = event(4);
        when(outboxEventRepository.findPendingEventsForUpdate()).thenReturn(List.of(event));
        doThrow(new KafkaPublishException("Falha Kafka", new RuntimeException()))
                .when(eventPublisher).publish(event);
        OutboxPoller poller = poller(5);

        // Execução: processa a última tentativa permitida.
        poller.publishPendingEvents();

        // Verificação: o evento não permanece elegível para novos polls.
        assertEquals(5, event.getRetryCount());
        assertEquals(OutboxProcessingStatus.FAILED, event.getProcessingStatus());
    }

    private OutboxPoller poller(int maxRetries) {
        return new OutboxPoller(
                outboxEventRepository,
                eventPublisher,
                maxRetries,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OutboxEventJpaEntity event(int retryCount) {
        return new OutboxEventJpaEntity(
                EVENT_ID,
                EMPLOYEE_ID,
                EmployeeStatus.ACTIVE,
                NOW,
                "EmployeeStatusChangedEvent",
                OutboxProcessingStatus.PENDING,
                NOW,
                null,
                retryCount);
    }
}
