package com.banco.hr.infrastructure.observability;

import com.banco.hr.domain.exception.DomainException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
public class OperationObservabilityAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationObservabilityAspect.class);
    private static final String TIMER_NAME = "hr.operation.duration";

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public OperationObservabilityAspect(MeterRegistry meterRegistry, Tracer tracer) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    @Around("""
            execution(public * com.banco.hr.application.usecase..*(..)) ||
            execution(public * com.banco.hr.infrastructure.adapters.outbound.messaging.OutboxPoller.publishPendingEvents(..)) ||
            execution(public * com.banco.hr.infrastructure.adapters.inbound.messaging.KafkaEmployeeEventConsumer.consume(..))
            """)
    public Object observeOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String component = AopUtils.getTargetClass(joinPoint.getTarget()).getSimpleName();
        String method = joinPoint.getSignature().getName();
        Timer.Sample sample = Timer.start(meterRegistry);
        Span span = tracer.nextSpan()
                .name(component + "." + method)
                .tag("component", component)
                .tag("method", method)
                .start();
        Throwable failure = null;

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            try {
                return joinPoint.proceed();
            } catch (Throwable exception) {
                failure = exception;
                span.error(exception);
                throw exception;
            } finally {
                String outcome = failure == null ? "success" : "failure";
                Timer timer = Timer.builder(TIMER_NAME)
                        .description("Duração das operações da aplicação e dos fluxos Kafka.")
                        .tag("component", component)
                        .tag("method", method)
                        .tag("outcome", outcome)
                        .publishPercentileHistogram()
                        .serviceLevelObjectives(
                                Duration.ofMillis(50),
                                Duration.ofMillis(100),
                                Duration.ofMillis(250),
                                Duration.ofMillis(500),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(5))
                        .register(meterRegistry);
                double durationMillis = sample.stop(timer) / 1_000_000.0;

                if (failure == null) {
                    log.info("Operação concluída",
                            StructuredArguments.kv("component", component),
                            StructuredArguments.kv("method", method),
                            StructuredArguments.kv("outcome", outcome),
                            StructuredArguments.kv("duration_ms", durationMillis));
                } else if (failure instanceof DomainException) {
                    log.warn("Operação rejeitada por regra de negócio",
                            StructuredArguments.kv("component", component),
                            StructuredArguments.kv("method", method),
                            StructuredArguments.kv("outcome", outcome),
                            StructuredArguments.kv("duration_ms", durationMillis),
                            StructuredArguments.kv("exception_type", failure.getClass().getSimpleName()));
                } else {
                    log.error("Falha inesperada durante a operação",
                            StructuredArguments.kv("component", component),
                            StructuredArguments.kv("method", method),
                            StructuredArguments.kv("outcome", outcome),
                            StructuredArguments.kv("duration_ms", durationMillis),
                            StructuredArguments.kv("exception_type", failure.getClass().getSimpleName()),
                            failure);
                }
            }
        } finally {
            span.end();
        }
    }
}
