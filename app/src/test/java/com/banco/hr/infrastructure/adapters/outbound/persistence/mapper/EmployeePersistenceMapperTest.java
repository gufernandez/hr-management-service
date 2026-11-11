package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmployeePersistenceMapperTest {

    private static final UUID EMPLOYEE_ID =
            UUID.fromString("7d261066-3d41-47db-8f83-5745ac9e0ca7");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);

    private final EmployeePersistenceMapper mapper = new EmployeePersistenceMapper();

    @Test
    @DisplayName("Deve mapear todos os campos do agregado para a entidade JPA")
    void toEntityCopiesAllEmployeeFields() {
        // Preparação: monta agregado com associações UUID e status conhecidos.
        Employee employee = employee(EmployeeStatus.CREATED, null);

        // Execução: converte o agregado para o formato persistido.
        EmployeeJpaEntity entity = mapper.toEntity(employee);

        // Verificação: os campos devem ser preservados sem introduzir dependência JPA no domínio.
        assertEquals(EMPLOYEE_ID, entity.getId());
        assertEquals("Ana Silva", entity.getName());
        assertEquals("ana@example.com", entity.getEmail());
        assertEquals(DEPARTMENT_ID, entity.getDepartmentId());
        assertEquals(POSITION_ID, entity.getPositionId());
        assertEquals(EmployeeStatus.CREATED, entity.getStatus());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(CREATED_AT, entity.getLastUpdatedAt());
        assertNull(entity.getDeletedAt());
    }

    @Test
    @DisplayName("Deve reconstruir agregado excluído com a data de exclusão")
    void toDomainReconstitutesDeletedEmployeeWithAllFields() {
        // Preparação: cria entidade com os dados persistidos de um colaborador excluído.
        EmployeeJpaEntity entity = new EmployeeJpaEntity(
                EMPLOYEE_ID,
                "Ana Silva",
                "ana@example.com",
                DEPARTMENT_ID,
                POSITION_ID,
                EmployeeStatus.DELETED,
                CREATED_AT,
                UPDATED_AT,
                UPDATED_AT);

        // Execução: remapeia a entidade para o modelo de domínio.
        Employee employee = mapper.toDomain(entity);

        // Verificação: IDs, status e timestamps são reconstituídos corretamente.
        assertEquals(new EmployeeId(EMPLOYEE_ID), employee.id());
        assertEquals(new EmployeeName("Ana Silva"), employee.name());
        assertEquals(new Email("ana@example.com"), employee.email());
        assertEquals(new DepartmentId(DEPARTMENT_ID), employee.departmentId());
        assertEquals(new PositionId(POSITION_ID), employee.positionId());
        assertEquals(EmployeeStatus.DELETED, employee.status());
        assertEquals(CREATED_AT, employee.createdAt());
        assertEquals(UPDATED_AT, employee.lastUpdatedAt());
        assertEquals(UPDATED_AT, employee.deletedAt());
    }

    private Employee employee(EmployeeStatus status, Instant deletedAt) {
        return Employee.reconstitute(
                new EmployeeId(EMPLOYEE_ID),
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                new DepartmentId(DEPARTMENT_ID),
                new PositionId(POSITION_ID),
                status,
                CREATED_AT,
                status == EmployeeStatus.CREATED ? CREATED_AT : UPDATED_AT,
                deletedAt);
    }
}
