package com.banco.hr.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationObservabilityAspectTest {

    private SimpleMeterRegistry meterRegistry;
    private Tracer tracer;
    private Span span;
    private OperationObservabilityAspect aspect;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        Tracer.SpanInScope spanInScope = mock(Tracer.SpanInScope.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.tag(anyString(), anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(spanInScope);
        aspect = new OperationObservabilityAspect(meterRegistry, tracer);
    }

    @Test
    @DisplayName("Registra duração e resultado quando a operação termina com sucesso")
    void observeOperation_WhenOperationSucceeds_RecordsSuccessTimer() throws Throwable {
        ProceedingJoinPoint joinPoint = createJoinPoint("execute");
        Object result = new SampleComponent();
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.observeOperation(joinPoint);

        assertThat(actual).isSameAs(result);
        assertThat(timer("SampleComponent", "execute", "success").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Registra falha e propaga a exceção original quando a operação falha")
    void observeOperation_WhenOperationFails_RecordsFailureAndRethrows() throws Throwable {
        ProceedingJoinPoint joinPoint = createJoinPoint("execute");
        IllegalStateException failure = new IllegalStateException("Falha simulada");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.observeOperation(joinPoint))
                .isSameAs(failure);

        assertThat(timer("SampleComponent", "execute", "failure").count()).isEqualTo(1);
    }

    private ProceedingJoinPoint createJoinPoint(String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(new SampleComponent());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        return joinPoint;
    }

    private io.micrometer.core.instrument.Timer timer(
            String component,
            String method,
            String outcome) {
        return meterRegistry.get("hr.operation.duration")
                .tag("component", component)
                .tag("method", method)
                .tag("outcome", outcome)
                .timer();
    }

    private static class SampleComponent {
    }
}
