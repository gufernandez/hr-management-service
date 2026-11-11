package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.exception.EmployeeNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.domain.model.position.PositionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeStatusUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final UUID EMPLOYEE_UUID =
            UUID.fromString("7d261066-3d41-47db-8f83-5745ac9e0ca7");
    private static final EmployeeId EMPLOYEE_ID = new EmployeeId(EMPLOYEE_UUID);

    @Mock
    private EmployeeRepositoryPort employeeRepository;

    private ActivateEmployeeUseCaseImpl activateUseCase;
    private DeactivateEmployeeUseCaseImpl deactivateUseCase;
    private DeleteEmployeeUseCaseImpl deleteUseCase;

    @BeforeEach
    void setUp() {
        // Preparação: todos os casos de mudança de status usam o mesmo relógio fixo.
        Clock clock = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);
        activateUseCase = new ActivateEmployeeUseCaseImpl(employeeRepository, clock);
        deactivateUseCase = new DeactivateEmployeeUseCaseImpl(employeeRepository, clock);
        deleteUseCase = new DeleteEmployeeUseCaseImpl(employeeRepository, clock);
    }

    @Test
    @DisplayName("Deve ativar colaborador CREATED ou INACTIVE")
    void activateEmployeePersistsActiveStatus() {
        // Preparação: fornece colaborador INACTIVE e retorna o agregado enviado para salvar.
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employeeWithStatus(EmployeeStatus.INACTIVE)));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: aplica a transição permitida de INACTIVE para ACTIVE.
        Employee result = activateUseCase.activateEmployee(EMPLOYEE_UUID);

        // Verificação: persiste status e timestamp atualizados.
        assertEquals(EmployeeStatus.ACTIVE, result.status());
        assertEquals(UPDATED_AT, result.lastUpdatedAt());
        verify(employeeRepository).save(result);
    }

    @Test
    @DisplayName("Deve desativar colaborador CREATED ou ACTIVE")
    void deactivateEmployeePersistsInactiveStatus() {
        // Preparação: fornece colaborador ACTIVE e retorna o agregado enviado para salvar.
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employeeWithStatus(EmployeeStatus.ACTIVE)));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: aplica a transição permitida de ACTIVE para INACTIVE.
        Employee result = deactivateUseCase.deactivateEmployee(EMPLOYEE_UUID);

        // Verificação: persiste status e timestamp atualizados.
        assertEquals(EmployeeStatus.INACTIVE, result.status());
        assertEquals(UPDATED_AT, result.lastUpdatedAt());
        verify(employeeRepository).save(result);
    }

    @Test
    @DisplayName("Deve excluir logicamente colaborador")
    void deleteEmployeePersistsDeletedStatusAndTimestamp() {
        // Preparação: fornece colaborador ativo para exclusão lógica.
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employeeWithStatus(EmployeeStatus.ACTIVE)));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: marca colaborador como DELETED sem removê-lo do repositório.
        Employee result = deleteUseCase.deleteEmployee(EMPLOYEE_UUID);

        // Verificação: status, data de exclusão e última atualização são persistidos.
        assertEquals(EmployeeStatus.DELETED, result.status());
        assertEquals(UPDATED_AT, result.deletedAt());
        assertEquals(UPDATED_AT, result.lastUpdatedAt());
        verify(employeeRepository).save(result);
    }

    @Test
    @DisplayName("Deve rejeitar alteração de status para colaborador inexistente")
    void statusUseCasesRejectUnknownEmployee() {
        // Preparação: nenhum colaborador é encontrado pelo identificador informado.
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        // Verificação: todas as operações de status sinalizam ausência sem tentar salvar.
        assertThrows(
                EmployeeNotFoundException.class,
                () -> activateUseCase.activateEmployee(EMPLOYEE_UUID));
        assertThrows(
                EmployeeNotFoundException.class,
                () -> deactivateUseCase.deactivateEmployee(EMPLOYEE_UUID));
        assertThrows(
                EmployeeNotFoundException.class,
                () -> deleteUseCase.deleteEmployee(EMPLOYEE_UUID));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar transição inválida sem persistir alteração")
    void statusUseCasesRejectInvalidTransitions() {
        // Preparação: ACTIVE não pode ser ativado de novo.
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employeeWithStatus(EmployeeStatus.ACTIVE)));

        // Verificação: transição fora das regras do agregado deve falhar sem salvar.
        assertThrows(
                DomainException.class,
                () -> activateUseCase.activateEmployee(EMPLOYEE_UUID));
        verify(employeeRepository, never()).save(any());
    }

    private Employee employeeWithStatus(EmployeeStatus status) {
        Instant deletedAt = status == EmployeeStatus.DELETED ? UPDATED_AT : null;
        return Employee.reconstitute(
                EMPLOYEE_ID,
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                new DepartmentId(UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397")),
                new PositionId(UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d")),
                status,
                CREATED_AT,
                status == EmployeeStatus.CREATED ? CREATED_AT : UPDATED_AT,
                deletedAt);
    }
}
