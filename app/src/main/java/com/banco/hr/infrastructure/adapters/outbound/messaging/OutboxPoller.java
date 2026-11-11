package com.banco.hr.infrastructure.adapters.outbound.messaging;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "outbox.poller.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventJpaRepository outboxEventRepository;
    private final KafkaOutboxEventPublisher eventPublisher;
    private final int maxRetries;
    private final Clock clock;

    @Autowired
    public OutboxPoller(
            OutboxEventJpaRepository outboxEventRepository,
            KafkaOutboxEventPublisher eventPublisher,
            @Value("${outbox.poller.max-retries:5}") int maxRetries) {
        this(outboxEventRepository, eventPublisher, maxRetries, Clock.systemUTC());
    }

    OutboxPoller(
            OutboxEventJpaRepository outboxEventRepository,
            KafkaOutboxEventPublisher eventPublisher,
            int maxRetries,
            Clock clock) {
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        if (maxRetries < 1) {
            throw new IllegalArgumentException("O máximo de retries deve ser maior que zero.");
        }
        this.maxRetries = maxRetries;
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(
            fixedDelayString = "${outbox.poller.interval:5000}",
            initialDelayString = "${outbox.poller.initial-delay:0}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEventJpaEntity event : outboxEventRepository.findPendingEventsForUpdate()) {
            try {
                eventPublisher.publish(event);
                event.markPublished(Instant.now(clock));
                log.info("Evento de outbox publicado: eventId={}, employeeId={}",
                        event.getEventId(), event.getEmployeeId());
            } catch (KafkaPublishException exception) {
                event.incrementRetryCount();
                if (event.getRetryCount() >= maxRetries) {
                    event.markFailed();
                    log.error("Evento de outbox excedeu o máximo de retries: eventId={}, retryCount={}",
                            event.getEventId(), event.getRetryCount(), exception);
                } else {
                    log.warn("Falha ao publicar evento de outbox: eventId={}, retryCount={}",
                            event.getEventId(), event.getRetryCount(), exception);
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        }
    }
}
