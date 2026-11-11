package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.domain.model.employee.EmployeeEvent;
import com.banco.hr.domain.model.employee.EmployeeEventType;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeEventJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployeeEventRepositoryAdapterTest {

    private static final UUID EVENT_ID = UUID.fromString("aa1f663d-6415-4121-bb14-94ba8e7977b0");
    private static final UUID EMPLOYEE_ID = UUID.fromString("5b2a1c80-d70c-4a56-a0c9-cdd0b16b5d7e");
    private static final Instant EVENT_TIME = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private EmployeeEventJpaRepository employeeEventJpaRepository;

    @Test
    @DisplayName("Deve mapear e persistir o evento de domínio no histórico JPA")
    void mapsAndPersistsEmployeeEvent() {
        // Preparação: cria evento de domínio com os dados do contrato de histórico.
        EmployeeEventRepositoryAdapter adapter =
                new EmployeeEventRepositoryAdapter(employeeEventJpaRepository);
        EmployeeEvent event = new EmployeeEvent(
                EVENT_ID,
                EMPLOYEE_ID,
                EVENT_TIME,
                EmployeeEventType.UPDATE,
                EmployeeStatus.INACTIVE);

        // Execução: delega o evento convertido para o repositório JPA.
        adapter.save(event);

        // Verificação: todas as propriedades do histórico são preservadas no mapeamento.
        ArgumentCaptor<EmployeeEventJpaEntity> entityCaptor =
                ArgumentCaptor.forClass(EmployeeEventJpaEntity.class);
        verify(employeeEventJpaRepository).saveAndFlush(entityCaptor.capture());
        EmployeeEventJpaEntity entity = entityCaptor.getValue();
        assertEquals(EVENT_ID, entity.getEventId());
        assertEquals(EMPLOYEE_ID, entity.getEmployeeId());
        assertEquals(EVENT_TIME, entity.getEventTime());
        assertEquals(EmployeeEventType.UPDATE, entity.getEventType());
        assertEquals(EmployeeStatus.INACTIVE, entity.getEmployeeStatus());
    }
}
