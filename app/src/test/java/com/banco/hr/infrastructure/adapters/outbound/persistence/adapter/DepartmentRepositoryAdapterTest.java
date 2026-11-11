package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.domain.model.department.CostCenterCode;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.DepartmentPersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.DepartmentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentRepositoryAdapterTest {

    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private DepartmentJpaRepository departmentJpaRepository;

    @Mock
    private DepartmentPersistenceMapper departmentPersistenceMapper;

    @InjectMocks
    private DepartmentRepositoryAdapter departmentRepositoryAdapter;

    @Test
    @DisplayName("Deve consultar no repositório JPA usando o nome do departamento")
    void existsByNameDelegatesNameToJpaRepository() {
        // Preparação: simula que o nome consultado existe na persistência.
        DepartmentName name = new DepartmentName("People");
        when(departmentJpaRepository.existsByName("People")).thenReturn(true);

        // Execução: consulta a existência por meio da porta de repositório.
        boolean exists = departmentRepositoryAdapter.existsByName(name);

        // Verificação: resultado e parâmetro encaminhado ao repositório JPA devem ser preservados.
        assertTrue(exists);
        verify(departmentJpaRepository).existsByName("People");
    }

    @Test
    @DisplayName("Deve retornar falso quando o nome não existe na persistência")
    void existsByNameReturnsFalseWhenJpaRepositoryDoesNotFindName() {
        // Preparação: simula resultado negativo da consulta no repositório JPA.
        when(departmentJpaRepository.existsByName("People")).thenReturn(false);

        // Execução: consulta a existência do nome de departamento.
        boolean exists = departmentRepositoryAdapter.existsByName(new DepartmentName("People"));

        // Verificação: o adaptador deve retornar o resultado do repositório sem alterá-lo.
        assertFalse(exists);
        verify(departmentJpaRepository).existsByName("People");
    }

    @Test
    @DisplayName("Deve verificar existência do departamento pelo UUID")
    void existsByIdDelegatesIdToJpaRepository() {
        // Preparação: simula que o departamento referenciado existe no banco.
        when(departmentJpaRepository.existsById(DEPARTMENT_ID)).thenReturn(true);

        // Execução: valida a referência pelo contrato de lookup do domínio.
        boolean exists = departmentRepositoryAdapter.existsById(new DepartmentId(DEPARTMENT_ID));

        // Verificação: o identificador é encaminhado ao repositório JPA.
        assertTrue(exists);
        verify(departmentJpaRepository).existsById(DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Deve resolver o identificador de departamento pelo nome")
    void findIdByNameReturnsDepartmentIdWhenFound() {
        // Preparação: configura a entidade encontrada pelo nome normalizado.
        DepartmentJpaEntity entity =
                new DepartmentJpaEntity(DEPARTMENT_ID, "People", "CC-10", CREATED_AT);
        when(departmentJpaRepository.findByName("People")).thenReturn(Optional.of(entity));

        // Execução: resolve o identificador para a busca de colaboradores.
        Optional<DepartmentId> result =
                departmentRepositoryAdapter.findIdByName(new DepartmentName("People"));

        // Verificação: retorna o UUID do departamento existente.
        assertEquals(Optional.of(new DepartmentId(DEPARTMENT_ID)), result);
        verify(departmentJpaRepository).findByName("People");
    }

    @Test
    @DisplayName("Deve mapear, persistir e devolver o departamento salvo")
    void saveMapsAndPersistsEntityThenReturnsMappedDepartment() {
        // Preparação: configura o fluxo esperado entre domínio, mapper e repositório JPA.
        Department department = Department.reconstitute(
                new DepartmentId(DEPARTMENT_ID),
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);
        DepartmentJpaEntity entity =
                new DepartmentJpaEntity(DEPARTMENT_ID, "People", "CC-10", CREATED_AT);
        DepartmentJpaEntity savedEntity =
                new DepartmentJpaEntity(DEPARTMENT_ID, "People", "CC-10", CREATED_AT);
        Department savedDepartment = Department.reconstitute(
                new DepartmentId(DEPARTMENT_ID),
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);
        when(departmentPersistenceMapper.toEntity(department)).thenReturn(entity);
        when(departmentJpaRepository.save(entity)).thenReturn(savedEntity);
        when(departmentPersistenceMapper.toDomain(savedEntity)).thenReturn(savedDepartment);

        // Execução: salva o agregado pela porta implementada pelo adaptador.
        Department result = departmentRepositoryAdapter.save(department);

        // Verificação: retorna o modelo reconstruído e executa as conversões e persistência na ordem lógica.
        assertSame(savedDepartment, result);
        verify(departmentPersistenceMapper).toEntity(department);
        verify(departmentJpaRepository).save(entity);
        verify(departmentPersistenceMapper).toDomain(savedEntity);
    }

    @Test
    @DisplayName("Deve propagar falha do repositório JPA ao salvar departamento")
    void savePropagatesJpaRepositoryFailure() {
        // Preparação: configura a entidade mapeada e uma falha de persistência conhecida.
        Department department = Department.reconstitute(
                new DepartmentId(DEPARTMENT_ID),
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);
        DepartmentJpaEntity entity =
                new DepartmentJpaEntity(DEPARTMENT_ID, "People", "CC-10", CREATED_AT);
        IllegalStateException failure = new IllegalStateException("Database failure");
        when(departmentPersistenceMapper.toEntity(department)).thenReturn(entity);
        when(departmentJpaRepository.save(entity)).thenThrow(failure);

        // Verificação: o adaptador deve propagar a falha em vez de retornar sucesso aparente.
        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> departmentRepositoryAdapter.save(department));

        assertSame(failure, result);
    }
}
