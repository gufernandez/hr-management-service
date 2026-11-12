package com.banco.hr.infrastructure.observability;

import java.time.Duration;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import com.banco.hr.domain.exception.DomainException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import net.logstash.logback.argument.StructuredArguments;

@Aspect
@Component
public class OperationObservabilityAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationObservabilityAspect.class);
    private static final String TIMER_NAME = "hr.operation.duration";
    private static final String COMPONENT_TAG = "component";
    private static final String METHOD_TAG = "method";
    private static final String OUTCOME_TAG = "outcome";
    private static final String DURATION_MILLIS_TAG = "duration_ms";
    private static final String EXCEPTION_TYPE_TAG = "exception_type";

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
        Object target = Objects.requireNonNull(joinPoint.getTarget(), "O alvo da operação não pode ser nulo.");
        String component = AopUtils.getTargetClass(target).getSimpleName();
        String method = joinPoint.getSignature().getName();
        Timer.Sample sample = Timer.start(meterRegistry);
        Span span = tracer.nextSpan()
                .name(component + "." + method)
                .tag(COMPONENT_TAG, component)
                .tag(METHOD_TAG, method)
                .start();
        Throwable failure = null;
        Tracer.SpanInScope spanInScope = tracer.withSpan(span);

        try {
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
                        .tag(COMPONENT_TAG, component)
                        .tag(METHOD_TAG, method)
                        .tag(OUTCOME_TAG, outcome)
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
                    log.info("Operação concluída {} {} {} {}",
                            StructuredArguments.kv(COMPONENT_TAG, component),
                            StructuredArguments.kv(METHOD_TAG, method),
                            StructuredArguments.kv(OUTCOME_TAG, outcome),
                            StructuredArguments.kv(DURATION_MILLIS_TAG, durationMillis));
                } else if (failure instanceof DomainException) {
                    log.warn("Operação rejeitada por regra de negócio {} {} {} {} {}",
                            StructuredArguments.kv(COMPONENT_TAG, component),
                            StructuredArguments.kv(METHOD_TAG, method),
                            StructuredArguments.kv(OUTCOME_TAG, outcome),
                            StructuredArguments.kv(DURATION_MILLIS_TAG, durationMillis),
                            StructuredArguments.kv(
                                    EXCEPTION_TYPE_TAG, failure.getClass().getSimpleName()));
                } else {
                    log.error("Falha inesperada durante a operação {} {} {} {} {}",
                            StructuredArguments.kv(COMPONENT_TAG, component),
                            StructuredArguments.kv(METHOD_TAG, method),
                            StructuredArguments.kv(OUTCOME_TAG, outcome),
                            StructuredArguments.kv(DURATION_MILLIS_TAG, durationMillis),
                            StructuredArguments.kv(
                                    EXCEPTION_TYPE_TAG, failure.getClass().getSimpleName()),
                            failure);
                }
            }
        } finally {
            try {
                spanInScope.close();
            } finally {
                span.end();
            }
        }
    }
}
