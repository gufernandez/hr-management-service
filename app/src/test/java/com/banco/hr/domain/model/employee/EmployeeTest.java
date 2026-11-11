package com.banco.hr.domain.model.employee;

import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.position.PositionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final DepartmentId DEPARTMENT_ID =
            new DepartmentId(UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397"));
    private static final PositionId POSITION_ID =
            new PositionId(UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d"));

    @Test
    @DisplayName("Deve criar colaborador no status CREATED com timestamps iniciais")
    void createInitializesCreatedStatusAndTimestamps() {
        // Preparação: cria colaborador com todos os dados e associações obrigatórias.
        Employee employee = Employee.create(
                new EmployeeName(" Ana Silva "),
                new Email(" ANA@EXAMPLE.COM "),
                DEPARTMENT_ID,
                POSITION_ID,
                CREATED_AT);

        // Verificação: o agregado gera ID, normaliza valores e inicia status e datas.
        assertNotNull(employee.id().value());
        assertEquals("Ana Silva", employee.name().value());
        assertEquals("ana@example.com", employee.email().value());
        assertEquals(DEPARTMENT_ID, employee.departmentId());
        assertEquals(POSITION_ID, employee.positionId());
        assertEquals(EmployeeStatus.CREATED, employee.status());
        assertEquals(CREATED_AT, employee.createdAt());
        assertEquals(CREATED_AT, employee.lastUpdatedAt());
        assertNull(employee.deletedAt());
    }

    @Test
    @DisplayName("Deve validar formato do e-mail e normalizá-lo sem diferenciar caixa")
    void emailRejectsInvalidValueAndNormalizesValidValue() {
        // Verificação: e-mail válido é normalizado para a forma usada na unicidade.
        assertEquals("ana.silva@example.com", new Email(" Ana.Silva@Example.COM ").value());

        // Verificação: valores ausentes ou sem formato de e-mail são rejeitados pelo domínio.
        assertThrows(DomainException.class, () -> new Email(null));
        assertThrows(DomainException.class, () -> new Email("  "));
        assertThrows(DomainException.class, () -> new Email("not-an-email"));
        assertThrows(DomainException.class, () -> new Email("missing-domain@"));
    }

    @Test
    @DisplayName("Deve ativar colaborador CREATED ou INACTIVE")
    void activateTransitionsCreatedAndInactiveToActive() {
        // Preparação: cria estados de origem permitidos para ativação.
        Employee created = newEmployee();
        Employee inactive = employeeWithStatus(EmployeeStatus.INACTIVE);

        // Verificação: ativação retorna agregados atualizados sem alterar os originais.
        assertEquals(EmployeeStatus.ACTIVE, created.activate(UPDATED_AT).status());
        assertEquals(EmployeeStatus.CREATED, created.status());
        assertEquals(EmployeeStatus.ACTIVE, inactive.activate(UPDATED_AT).status());
    }

    @Test
    @DisplayName("Deve desativar colaborador CREATED ou ACTIVE")
    void deactivateTransitionsCreatedAndActiveToInactive() {
        // Preparação: cria estados de origem permitidos para desativação.
        Employee created = newEmployee();
        Employee active = employeeWithStatus(EmployeeStatus.ACTIVE);

        // Verificação: desativação retorna agregados atualizados sem alterar os originais.
        assertEquals(EmployeeStatus.INACTIVE, created.deactivate(UPDATED_AT).status());
        assertEquals(EmployeeStatus.CREATED, created.status());
        assertEquals(EmployeeStatus.INACTIVE, active.deactivate(UPDATED_AT).status());
    }

    @Test
    @DisplayName("Deve rejeitar ativação e desativação a partir de estados não permitidos")
    void statusChangesRejectDisallowedSourceStates() {
        // Preparação: estados ACTIVE e DELETED não podem ser ativados novamente.
        Employee active = employeeWithStatus(EmployeeStatus.ACTIVE);
        Employee deleted = employeeWithStatus(EmployeeStatus.DELETED);

        // Verificação: transições fora da regra de domínio falham explicitamente.
        assertThrows(DomainException.class, () -> active.activate(UPDATED_AT));
        assertThrows(DomainException.class, () -> deleted.activate(UPDATED_AT));
        assertThrows(DomainException.class,
                () -> employeeWithStatus(EmployeeStatus.INACTIVE).deactivate(UPDATED_AT));
        assertThrows(DomainException.class, () -> deleted.deactivate(UPDATED_AT));
    }

    @Test
    @DisplayName("Deve excluir logicamente colaborador e registrar a data de exclusão")
    void deleteSetsDeletedStatusAndTimestamps() {
        // Preparação: cria colaborador ainda não excluído.
        Employee employee = newEmployee();

        // Verificação: exclusão preserva a data original e preenche atualização/exclusão.
        Employee deleted = employee.delete(UPDATED_AT);
        assertEquals(EmployeeStatus.DELETED, deleted.status());
        assertEquals(CREATED_AT, deleted.createdAt());
        assertEquals(UPDATED_AT, deleted.lastUpdatedAt());
        assertEquals(UPDATED_AT, deleted.deletedAt());
        assertEquals(EmployeeStatus.CREATED, employee.status());
        assertNull(employee.deletedAt());
    }

    @Test
    @DisplayName("Deve rejeitar exclusão repetida e atualização retroativa")
    void deleteRejectsRepeatedDeletionAndBackdatedUpdate() {
        // Preparação: constrói colaborador já excluído e colaborador atualizado.
        Employee deleted = employeeWithStatus(EmployeeStatus.DELETED);
        Employee active = employeeWithStatus(EmployeeStatus.ACTIVE);

        // Verificação: o domínio rejeita exclusão repetida e timestamp anterior à atualização.
        assertThrows(DomainException.class, () -> deleted.delete(UPDATED_AT));
        assertThrows(DomainException.class, () -> active.activate(CREATED_AT));
    }

    private Employee newEmployee() {
        return Employee.create(
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                DEPARTMENT_ID,
                POSITION_ID,
                CREATED_AT);
    }

    private Employee employeeWithStatus(EmployeeStatus status) {
        Instant deletedAt = status == EmployeeStatus.DELETED ? UPDATED_AT : null;
        return Employee.reconstitute(
                new EmployeeId(UUID.randomUUID()),
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                DEPARTMENT_ID,
                POSITION_ID,
                status,
                CREATED_AT,
                status == EmployeeStatus.CREATED ? CREATED_AT : UPDATED_AT,
                deletedAt);
    }
}
