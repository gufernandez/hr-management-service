package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.department.CostCenterCode;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepartmentPersistenceMapperTest {

    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    private final DepartmentPersistenceMapper mapper = new DepartmentPersistenceMapper();

    @Test
    @DisplayName("Deve mapear todos os campos do domínio para a entidade JPA")
    void toEntityCopiesAllDepartmentFields() {
        // Preparação: cria agregado com valores conhecidos para validar o mapeamento campo a campo.
        Department department = Department.reconstitute(
                new DepartmentId(DEPARTMENT_ID),
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);

        // Execução: converte o agregado para o formato persistido.
        DepartmentJpaEntity entity = mapper.toEntity(department);

        // Verificação: os valores persistidos devem corresponder aos dados do domínio.
        assertEquals(DEPARTMENT_ID, entity.getId());
        assertEquals("People", entity.getName());
        assertEquals("CC-10", entity.getCostCenterCode());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    @DisplayName("Deve reconstruir o agregado a partir de todos os campos da entidade JPA")
    void toDomainReconstitutesDepartmentWithAllEntityFields() {
        // Preparação: cria entidade com valores conhecidos como se viessem do banco.
        DepartmentJpaEntity entity =
                new DepartmentJpaEntity(DEPARTMENT_ID, "People", "CC-10", CREATED_AT);

        // Execução: reconstrói o modelo de domínio a partir da entidade.
        Department department = mapper.toDomain(entity);

        // Verificação: nenhum campo deve ser perdido na conversão para o domínio.
        assertEquals(new DepartmentId(DEPARTMENT_ID), department.id());
        assertEquals(new DepartmentName("People"), department.name());
        assertEquals(new CostCenterCode("CC-10"), department.costCenterCode());
        assertEquals(CREATED_AT, department.createdAt());
    }
}
