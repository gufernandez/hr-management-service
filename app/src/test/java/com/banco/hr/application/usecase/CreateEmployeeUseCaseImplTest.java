package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.dto.CreateEmployeeCommand;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.application.ports.outbound.PositionLookupPort;
import com.banco.hr.domain.exception.DepartmentNotFoundException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.exception.PositionNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final UUID DEPARTMENT_UUID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final UUID POSITION_UUID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");

    @Mock
    private EmployeeRepositoryPort employeeRepository;

    @Mock
    private DepartmentLookupPort departmentLookup;

    @Mock
    private PositionLookupPort positionLookup;

    private CreateEmployeeUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        // Preparação: congela o relógio para verificar os timestamps do agregado.
        useCase = new CreateEmployeeUseCaseImpl(
                employeeRepository,
                departmentLookup,
                positionLookup,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Deve validar os UUIDs de referência e salvar colaborador CREATED")
    void createEmployeeValidatesReferencesAndSavesCreatedEmployee() {
        // Preparação: disponibiliza as referências e indica que o e-mail está livre.
        CreateEmployeeCommand command = validCommand();
        when(departmentLookup.existsById(new DepartmentId(DEPARTMENT_UUID))).thenReturn(true);
        when(positionLookup.existsById(new PositionId(POSITION_UUID))).thenReturn(true);
        when(employeeRepository.existsByEmail(new Email("ana@example.com"))).thenReturn(false);
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: cria colaborador pela porta de entrada da aplicação.
        Employee result = useCase.createEmployee(command);

        // Verificação: o domínio recebe as referências UUID e o e-mail normalizado.
        assertNotNull(result.id().value());
        assertEquals("Ana Silva", result.name().value());
        assertEquals("ana@example.com", result.email().value());
        assertEquals(new DepartmentId(DEPARTMENT_UUID), result.departmentId());
        assertEquals(new PositionId(POSITION_UUID), result.positionId());
        assertEquals(EmployeeStatus.CREATED, result.status());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.lastUpdatedAt());
        verify(employeeRepository).existsByEmail(new Email("ana@example.com"));
        verify(employeeRepository).save(result);
    }

    @Test
    @DisplayName("Deve rejeitar e-mail duplicado sem salvar colaborador")
    void createEmployeeRejectsDuplicateEmail() {
        // Preparação: confirma referências e indica e-mail já cadastrado.
        when(departmentLookup.existsById(new DepartmentId(DEPARTMENT_UUID))).thenReturn(true);
        when(positionLookup.existsById(new PositionId(POSITION_UUID))).thenReturn(true);
        when(employeeRepository.existsByEmail(new Email("ana@example.com"))).thenReturn(true);

        // Verificação: unicidade de e-mail é case-insensitive e bloqueia a gravação.
        assertThrows(EmployeeAlreadyExistsException.class,
                () -> useCase.createEmployee(validCommand()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar departamento inexistente antes de verificar cargo")
    void createEmployeeRejectsMissingDepartment() {
        // Preparação: o UUID informado não corresponde a um departamento cadastrado.
        when(departmentLookup.existsById(new DepartmentId(DEPARTMENT_UUID))).thenReturn(false);

        // Verificação: não deve consultar o cargo nem gravar um colaborador sem departamento.
        assertThrows(DepartmentNotFoundException.class,
                () -> useCase.createEmployee(validCommand()));
        verify(positionLookup, never()).existsById(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar cargo inexistente sem salvar colaborador")
    void createEmployeeRejectsMissingPosition() {
        // Preparação: o departamento existe, mas o UUID do cargo não.
        when(departmentLookup.existsById(new DepartmentId(DEPARTMENT_UUID))).thenReturn(true);
        when(positionLookup.existsById(new PositionId(POSITION_UUID))).thenReturn(false);

        // Verificação: associação inválida interrompe o fluxo antes de persistir.
        assertThrows(PositionNotFoundException.class,
                () -> useCase.createEmployee(validCommand()));
        verify(employeeRepository, never()).existsByEmail(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar e-mail inválido antes de acessar as referências")
    void createEmployeeRejectsInvalidEmailBeforeLookup() {
        // Preparação: o e-mail inválido não pode iniciar consultas de cadastro.
        CreateEmployeeCommand command = new CreateEmployeeCommand(
                "Ana Silva", "invalid-email", DEPARTMENT_UUID, POSITION_UUID);

        // Verificação: a validação do value object precede consultas e persistência.
        assertThrows(DomainException.class, () -> useCase.createEmployee(command));
        verify(departmentLookup, never()).existsById(any());
        verify(positionLookup, never()).existsById(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar comando nulo sem acessar dependências")
    void createEmployeeRejectsNullCommand() {
        // Verificação: comando nulo é inválido e não deve iniciar operações externas.
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> useCase.createEmployee(null));
        assertNotNull(exception);
        verify(departmentLookup, never()).existsById(any());
        verify(positionLookup, never()).existsById(any());
        verify(employeeRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("Deve propagar falha de persistência")
    void createEmployeePropagatesRepositoryFailure() {
        // Preparação: referências válidas e falha explícita ao salvar.
        IllegalStateException failure = new IllegalStateException("Repository failure");
        when(departmentLookup.existsById(new DepartmentId(DEPARTMENT_UUID))).thenReturn(true);
        when(positionLookup.existsById(new PositionId(POSITION_UUID))).thenReturn(true);
        when(employeeRepository.existsByEmail(new Email("ana@example.com"))).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenThrow(failure);

        // Verificação: a falha não pode ser mascarada como criação bem-sucedida.
        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> useCase.createEmployee(validCommand()));
        assertSame(failure, result);
    }

    private CreateEmployeeCommand validCommand() {
        return new CreateEmployeeCommand(
                "Ana Silva", " ANA@EXAMPLE.COM ", DEPARTMENT_UUID, POSITION_UUID);
    }
}
