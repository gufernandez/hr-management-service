package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.EmployeePersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeJpaRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeRepositoryAdapterTest {

    private static final UUID EMPLOYEE_UUID =
            UUID.fromString("7d261066-3d41-47db-8f83-5745ac9e0ca7");
    private static final UUID DEPARTMENT_UUID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    @Mock
    private EmployeePersistenceMapper employeePersistenceMapper;

    @InjectMocks
    private EmployeeRepositoryAdapter employeeRepositoryAdapter;

    @Test
    @DisplayName("Deve consultar existência de e-mail usando o valor normalizado")
    void existsByEmailDelegatesNormalizedAddress() {
        // Preparação: simula endereço normalizado já persistido.
        when(employeeJpaRepository.existsByEmail("ana@example.com")).thenReturn(true);

        // Execução: consulta a existência através da porta de repositório.
        boolean exists = employeeRepositoryAdapter.existsByEmail(new Email(" ANA@EXAMPLE.COM "));

        // Verificação: o valor normalizado é delegado sem alterações.
        assertTrue(exists);
        verify(employeeJpaRepository).existsByEmail("ana@example.com");
    }

    @Test
    @DisplayName("Deve buscar colaborador pelo UUID")
    void findByIdMapsEntityWhenEmployeeExists() {
        // Preparação: configura entidade persistida e agregado correspondente.
        EmployeeJpaEntity entity = entity();
        Employee employee = employee();
        when(employeeJpaRepository.findById(EMPLOYEE_UUID)).thenReturn(Optional.of(entity));
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: busca pelo ID de domínio.
        Optional<Employee> result =
                employeeRepositoryAdapter.findById(new EmployeeId(EMPLOYEE_UUID));

        // Verificação: retorno é mapeado e identidade encaminhada corretamente.
        assertEquals(Optional.of(employee), result);
        verify(employeePersistenceMapper).toDomain(entity);
    }

    @Test
    @DisplayName("Deve mapear e persistir agregado Employee")
    void saveMapsEntityAndReturnsSavedAggregate() {
        // Preparação: configura conversões e retorno do repositório JPA.
        Employee employee = employee();
        EmployeeJpaEntity entity = entity();
        when(employeePersistenceMapper.toEntity(employee)).thenReturn(entity);
        when(employeeJpaRepository.save(entity)).thenReturn(entity);
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: salva o agregado pela porta de saída.
        Employee result = employeeRepositoryAdapter.save(employee);

        // Verificação: o agregado retornado corresponde à entidade salva.
        assertSame(employee, result);
        verify(employeeJpaRepository).save(entity);
        verify(employeePersistenceMapper).toDomain(entity);
    }

    @Test
    @DisplayName("Deve listar colaboradores não deletados sem paginação")
    void findAllNotDeletedMapsRepositoryResults() {
        // Preparação: configura a consulta ordenada com colaboradores elegíveis.
        EmployeeJpaEntity entity = entity();
        Employee employee = employee();
        when(employeeJpaRepository.findAllByStatusNotOrderByCreatedAtDesc(EmployeeStatus.DELETED))
                .thenReturn(List.of(entity));
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: solicita listagem completa pela porta.
        List<Employee> result = employeeRepositoryAdapter.findAllNotDeleted();

        // Verificação: status DELETED é excluído pelo critério encaminhado ao JPA.
        assertEquals(List.of(employee), result);
        verify(employeeJpaRepository)
                .findAllByStatusNotOrderByCreatedAtDesc(EmployeeStatus.DELETED);
    }

    @Test
    @DisplayName("Deve preservar offset e limite ao listar colaboradores paginados")
    void findAllNotDeletedPreservesOffsetAndLimit() {
        // Preparação: usa offset não múltiplo do limite para validar paginação exata.
        EmployeePageRequest request = new EmployeePageRequest(7, 5);
        EmployeeJpaEntity entity = entity();
        Employee employee = employee();
        when(employeeJpaRepository.findAllByStatusNot(eq(EmployeeStatus.DELETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: obtém a página através da porta.
        EmployeePage result = employeeRepositoryAdapter.findAllNotDeleted(request);

        // Verificação: offset literal, limite, total e conteúdo são preservados.
        assertEquals(7, result.offset());
        assertEquals(5, result.limit());
        assertEquals(1, result.total());
        assertEquals(List.of(employee), result.content());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeJpaRepository).findAllByStatusNot(
                eq(EmployeeStatus.DELETED), pageable.capture());
        assertEquals(7, pageable.getValue().getOffset());
        assertEquals(5, pageable.getValue().getPageSize());
    }

    @Test
    @DisplayName("Deve buscar página não deletada pelo departamento")
    void findNotDeletedByDepartmentUsesDepartmentUuidAndPagination() {
        // Preparação: configura departamento e página de resultado.
        EmployeePageRequest request = new EmployeePageRequest(0, 10);
        EmployeeJpaEntity entity = entity();
        Employee employee = employee();
        when(employeeJpaRepository.findAllByDepartmentIdAndStatusNot(
                eq(DEPARTMENT_UUID), eq(EmployeeStatus.DELETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: solicita a página para o UUID do departamento.
        EmployeePage result = employeeRepositoryAdapter.findNotDeletedByDepartment(
                new DepartmentId(DEPARTMENT_UUID), request);

        // Verificação: consulta restringe por departamento e status, mantendo o total.
        assertEquals(List.of(employee), result.content());
        assertEquals(1, result.total());
        verify(employeeJpaRepository).findAllByDepartmentIdAndStatusNot(
                eq(DEPARTMENT_UUID), eq(EmployeeStatus.DELETED), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve buscar todos os colaboradores não deletados por departamento")
    void findNotDeletedByDepartmentWithoutPaginationMapsResults() {
        // Preparação: configura lista ordenada retornada pelo repositório JPA.
        EmployeeJpaEntity entity = entity();
        Employee employee = employee();
        when(employeeJpaRepository.findAllByDepartmentIdAndStatusNotOrderByCreatedAtDesc(
                DEPARTMENT_UUID, EmployeeStatus.DELETED)).thenReturn(List.of(entity));
        when(employeePersistenceMapper.toDomain(entity)).thenReturn(employee);

        // Execução: consulta colaboradores sem fornecer parâmetros de paginação.
        List<Employee> result = employeeRepositoryAdapter.findNotDeletedByDepartment(
                new DepartmentId(DEPARTMENT_UUID));

        // Verificação: mapeia o conteúdo e restringe a consulta ao departamento não deletado.
        assertEquals(List.of(employee), result);
        verify(employeeJpaRepository).findAllByDepartmentIdAndStatusNotOrderByCreatedAtDesc(
                DEPARTMENT_UUID, EmployeeStatus.DELETED);
        verify(employeePersistenceMapper).toDomain(entity);
    }

    @Test
    @DisplayName("Deve traduzir violação de e-mail único para exceção de domínio")
    void saveTranslatesDuplicateEmailConstraintViolation() {
        // Preparação: simula inserção concorrente que já ocupou o mesmo e-mail.
        Employee employee = employee();
        EmployeeJpaEntity entity = entity();
        when(employeePersistenceMapper.toEntity(employee)).thenReturn(entity);
        when(employeeJpaRepository.save(entity))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));
        when(employeeJpaRepository.existsByEmail("ana@example.com")).thenReturn(true);
        when(employeeJpaRepository.findById(EMPLOYEE_UUID)).thenReturn(Optional.empty());

        // Verificação: a violação de e-mail é comunicada como regra de domínio.
        assertThrows(
                EmployeeAlreadyExistsException.class,
                () -> employeeRepositoryAdapter.save(employee));
    }

    @Test
    @DisplayName("Deve propagar violação de integridade que não seja e-mail duplicado")
    void savePropagatesUnrelatedIntegrityViolation() {
        // Preparação: simula falha de integridade sem colisão de e-mail.
        Employee employee = employee();
        EmployeeJpaEntity entity = entity();
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("Foreign key violation");
        when(employeePersistenceMapper.toEntity(employee)).thenReturn(entity);
        when(employeeJpaRepository.save(entity)).thenThrow(failure);
        when(employeeJpaRepository.existsByEmail("ana@example.com")).thenReturn(false);

        // Verificação: outras violações não podem ser convertidas em falso conflito.
        DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> employeeRepositoryAdapter.save(employee));
        assertSame(failure, result);
        verify(employeeJpaRepository, never()).findById(EMPLOYEE_UUID);
    }

    private EmployeeJpaEntity entity() {
        return new EmployeeJpaEntity(
                EMPLOYEE_UUID,
                "Ana Silva",
                "ana@example.com",
                DEPARTMENT_UUID,
                UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d"),
                EmployeeStatus.CREATED,
                CREATED_AT,
                CREATED_AT,
                null);
    }

    private Employee employee() {
        return Employee.reconstitute(
                new EmployeeId(EMPLOYEE_UUID),
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                new DepartmentId(DEPARTMENT_UUID),
                new PositionId(UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d")),
                EmployeeStatus.CREATED,
                CREATED_AT,
                CREATED_AT,
                null);
    }
}
