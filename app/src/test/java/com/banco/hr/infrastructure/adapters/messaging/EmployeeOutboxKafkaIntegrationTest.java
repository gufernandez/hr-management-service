package com.banco.hr.infrastructure.adapters.messaging;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.doReturn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.messaging.dto.KafkaEmployeeEvent;
import com.banco.hr.infrastructure.adapters.outbound.messaging.OutboxPoller;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxProcessingStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeEventJpaRepository;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.kafka.listener.auto-startup=true",
        "outbox.poller.enabled=true",
        "outbox.poller.interval=3600000",
        "outbox.poller.initial-delay=3600000",
        "outbox.poller.max-retries=5",
        "outbox.kafka.topic=hr.employee-events.v1"
})
@ActiveProfiles("test")
@Testcontainers
class EmployeeOutboxKafkaIntegrationTest {

    private static final String TOPIC = "hr.employee-events.v1";
    private static final String CONSUMER_GROUP = "employee-outbox-it-" + UUID.randomUUID();

    @Container
        private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @Container
    private static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private EmployeeEventJpaRepository employeeEventRepository;

    @SpyBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> CONSUMER_GROUP);
    }

    @BeforeAll
    static void createTopic() throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()))) {
            adminClient.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @BeforeEach
    void clearEventTables() {
        employeeEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve publicar eventos da outbox e persistir o histórico pelo consumer Kafka")
    void publishesOutboxEventsAndPersistsHistory() throws Exception {
        // Preparação: insere eventos de criação e alteração diretamente na tabela de outbox.
        Instant eventTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UUID createdEmployeeId = UUID.randomUUID();
        UUID createdEventId = UUID.randomUUID();
        UUID activeEmployeeId = UUID.randomUUID();
        UUID activeEventId = UUID.randomUUID();
        UUID inactiveEmployeeId = UUID.randomUUID();
        UUID inactiveEventId = UUID.randomUUID();
        UUID deletedEmployeeId = UUID.randomUUID();
        UUID deletedEventId = UUID.randomUUID();
        outboxEventRepository.saveAll(List.of(
                outboxEvent(
                        createdEventId,
                        createdEmployeeId,
                        EmployeeStatus.CREATED,
                        "EmployeeCreatedEvent",
                        eventTime),
                outboxEvent(
                        activeEventId,
                        activeEmployeeId,
                        EmployeeStatus.ACTIVE,
                        "EmployeeStatusChangedEvent",
                        eventTime.plusMillis(1)),
                outboxEvent(
                        inactiveEventId,
                        inactiveEmployeeId,
                        EmployeeStatus.INACTIVE,
                        "EmployeeStatusChangedEvent",
                        eventTime.plusMillis(2)),
                outboxEvent(
                        deletedEventId,
                        deletedEmployeeId,
                        EmployeeStatus.DELETED,
                        "EmployeeStatusChangedEvent",
                        eventTime.plusMillis(3))));

        // Execução: poller publica no Kafka e o listener consome as mensagens.
        outboxPoller.publishPendingEvents();

        // Verificação: ACK do producer, status de outbox e persistência pelo consumer.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertEquals(
                    OutboxProcessingStatus.PUBLISHED,
                    outboxEventRepository.findById(createdEventId)
                            .orElseThrow()
                            .getProcessingStatus());
            assertEquals(OutboxProcessingStatus.PUBLISHED,
                    outboxEventRepository.findById(activeEventId).orElseThrow().getProcessingStatus());
            assertEquals(OutboxProcessingStatus.PUBLISHED,
                    outboxEventRepository.findById(inactiveEventId).orElseThrow().getProcessingStatus());
            assertEquals(OutboxProcessingStatus.PUBLISHED,
                    outboxEventRepository.findById(deletedEventId).orElseThrow().getProcessingStatus());
            assertEquals(4, employeeEventRepository.count());
        });
        awaitConsumerAtTopicEnd();

        EmployeeEventJpaEntity createdEvent = employeeEventRepository.findById(createdEventId)
                .orElseThrow();
        assertEquals(createdEmployeeId, createdEvent.getEmployeeId());
        assertEquals(eventTime, createdEvent.getEventTime());
        assertEquals("CREATION", createdEvent.getEventType().name());
        assertEquals(EmployeeStatus.CREATED, createdEvent.getEmployeeStatus());

        EmployeeEventJpaEntity activeEvent = employeeEventRepository.findById(activeEventId)
                .orElseThrow();
        assertEquals(activeEmployeeId, activeEvent.getEmployeeId());
        assertEquals(eventTime.plusMillis(1), activeEvent.getEventTime());
        assertEquals("UPDATE", activeEvent.getEventType().name());
        assertEquals(EmployeeStatus.ACTIVE, activeEvent.getEmployeeStatus());

        assertStatusChangeEvent(inactiveEventId, inactiveEmployeeId, eventTime.plusMillis(2),
                EmployeeStatus.INACTIVE);
        assertStatusChangeEvent(deletedEventId, deletedEmployeeId, eventTime.plusMillis(3),
                EmployeeStatus.DELETED);

        assertNotNull(outboxEventRepository.findById(createdEventId).orElseThrow().getPublishedAt());
        assertNotNull(outboxEventRepository.findById(activeEventId).orElseThrow().getPublishedAt());
        assertNotNull(outboxEventRepository.findById(inactiveEventId).orElseThrow().getPublishedAt());
        assertNotNull(outboxEventRepository.findById(deletedEventId).orElseThrow().getPublishedAt());
    }

    @Test
    @DisplayName("Deve marcar evento como FAILED ao atingir o máximo de falhas de publicação")
    void marksOutboxEventFailedWhenMaximumRetriesIsReached() {
        // Preparação: força falha do publisher para um evento específico da outbox.
        UUID eventId = UUID.randomUUID();
        OutboxEventJpaEntity event = outboxEvent(
                eventId,
                UUID.randomUUID(),
                EmployeeStatus.CREATED,
                "EmployeeCreatedEvent",
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        outboxEventRepository.save(event);
        doReturn(CompletableFuture.failedFuture(
                new KafkaException("Falha Kafka simulada")))
                .when(kafkaTemplate)
                .send(ArgumentMatchers.<ProducerRecord<String, String>>argThat(record ->
                        record != null && event.getEmployeeId().toString().equals(record.key())));

        // Execução: executa o poller uma vez por tentativa permitida.
        for (int attempt = 1; attempt <= 5; attempt++) {
            outboxPoller.publishPendingEvents();
            OutboxEventJpaEntity persisted = outboxEventRepository.findById(eventId)
                    .orElseThrow();
            assertEquals(attempt, persisted.getRetryCount());
            if (attempt < 5) {
                assertEquals(OutboxProcessingStatus.PENDING, persisted.getProcessingStatus());
            }
        }

        // Verificação: após a quinta falha não publica novamente nem cria histórico.
        OutboxEventJpaEntity failedEvent = outboxEventRepository.findById(eventId)
                .orElseThrow();
        assertEquals(OutboxProcessingStatus.FAILED, failedEvent.getProcessingStatus());
        assertEquals(5, failedEvent.getRetryCount());
        assertNull(failedEvent.getPublishedAt());
        assertEquals(0, employeeEventRepository.count());
    }

    @Test
    @DisplayName("Deve ignorar evento duplicado entregue novamente pelo Kafka")
    void doesNotPersistDuplicateKafkaEvent() throws Exception {
        // Preparação: o primeiro fluxo publica e persiste uma entrada normalmente.
        Instant eventTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UUID eventId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        OutboxEventJpaEntity outboxEvent = outboxEvent(
                eventId,
                employeeId,
                EmployeeStatus.CREATED,
                "EmployeeCreatedEvent",
                eventTime);
        outboxEventRepository.save(outboxEvent);

        // Execução: após o primeiro consumo, envia novamente o mesmo eventId ao tópico.
        outboxPoller.publishPendingEvents();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(
                () -> assertEquals(1, employeeEventRepository.count()));
        KafkaEmployeeEvent duplicate = new KafkaEmployeeEvent(
                eventId,
                "EmployeeCreatedEvent",
                employeeId,
                eventTime,
                new KafkaEmployeeEvent.Payload(employeeId, EmployeeStatus.CREATED));
        long duplicateOffset = kafkaTemplate.send(
                TOPIC,
                employeeId.toString(),
                objectMapper.writeValueAsString(duplicate))
                .get()
                .getRecordMetadata()
                .offset();

        // Verificação: o consumidor confirma a duplicata sem armazenar uma segunda linha.
        awaitConsumerOffset(duplicateOffset + 1);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(
                () -> assertEquals(1, employeeEventRepository.count()));
    }

    private void awaitConsumerAtTopicEnd() throws Exception {
        TopicPartition partition = new TopicPartition(TOPIC, 0);
        try (AdminClient adminClient = adminClient()) {
            long endOffset = adminClient.listOffsets(Map.of(partition, OffsetSpec.latest()))
                    .all()
                    .get()
                    .get(partition)
                    .offset();
            awaitConsumerOffset(adminClient, partition, endOffset);
        }
    }

    private void awaitConsumerOffset(long expectedOffset) throws Exception {
        try (AdminClient adminClient = adminClient()) {
            awaitConsumerOffset(
                    adminClient,
                    new TopicPartition(TOPIC, 0),
                    expectedOffset);
        }
    }

    private void awaitConsumerOffset(
            AdminClient adminClient,
            TopicPartition partition,
            long expectedOffset) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Map<TopicPartition, OffsetAndMetadata> offsets = adminClient
                    .listConsumerGroupOffsets(CONSUMER_GROUP)
                    .partitionsToOffsetAndMetadata()
                    .get();
            OffsetAndMetadata offset = offsets.get(partition);
            assertNotNull(offset);
            assertTrue(offset.offset() >= expectedOffset);
        });
    }

    private AdminClient adminClient() {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()));
    }

    private void assertStatusChangeEvent(
            UUID eventId,
            UUID employeeId,
            Instant eventTime,
            EmployeeStatus employeeStatus) {
        EmployeeEventJpaEntity event = employeeEventRepository.findById(eventId).orElseThrow();
        assertEquals(employeeId, event.getEmployeeId());
        assertEquals(eventTime, event.getEventTime());
        assertEquals("UPDATE", event.getEventType().name());
        assertEquals(employeeStatus, event.getEmployeeStatus());
    }

    private OutboxEventJpaEntity outboxEvent(
            UUID eventId,
            UUID employeeId,
            EmployeeStatus status,
            String eventType,
            Instant eventTime) {
        return new OutboxEventJpaEntity(
                eventId,
                employeeId,
                status,
                eventTime,
                eventType,
                OutboxProcessingStatus.PENDING,
                eventTime,
                null,
                0);
    }
}
