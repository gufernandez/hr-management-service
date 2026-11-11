package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.CreateDepartmentCommand;
import com.banco.hr.application.ports.outbound.DepartmentRepositoryPort;
import com.banco.hr.domain.exception.DepartmentAlreadyExistsException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDepartmentUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private DepartmentRepositoryPort departmentRepository;

    private CreateDepartmentUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        // Preparação: relógio fixo torna determinística a data criada pelo caso de uso.
        useCase = new CreateDepartmentUseCaseImpl(
                departmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Deve salvar e retornar departamento com ID gerado e data atual")
    void createDepartmentSavesAndReturnsDepartmentWithGeneratedIdAndTimestamp() {
        // Preparação: simula nome disponível e persistência bem-sucedida.
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: cria departamento por meio do caso de uso.
        Department result = useCase.createDepartment(command);

        // Verificação: confere os dados gerados e que a consulta e gravação foram realizadas.
        assertNotNull(result.id().value());
        assertEquals("People", result.name().value());
        assertEquals("CC-10", result.costCenterCode().value());
        assertEquals(NOW, result.createdAt());
        verify(departmentRepository).existsByName(new DepartmentName("People"));
        verify(departmentRepository).save(result);
    }

    @Test
    @DisplayName("Deve normalizar nome e código antes de consultar e salvar")
    void createDepartmentTrimsValuesBeforeCheckingAndSaving() {
        // Preparação: envia espaços extras para validar a normalização dos value objects.
        CreateDepartmentCommand command =
                new CreateDepartmentCommand("  People  ", "  CC-10  ");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: processa os valores informados no comando.
        Department result = useCase.createDepartment(command);

        // Verificação: a persistência e a consulta usam valores normalizados.
        assertEquals("People", result.name().value());
        assertEquals("CC-10", result.costCenterCode().value());
        verify(departmentRepository).existsByName(new DepartmentName("People"));
    }

    @Test
    @DisplayName("Deve rejeitar nome já cadastrado sem tentar salvar")
    void createDepartmentRejectsDuplicateNameWithoutSaving() {
        // Preparação: informa que o repositório já contém o nome solicitado.
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(true);

        // Verificação: duplicidade deve gerar exceção de domínio e impedir a gravação.
        assertThrows(DepartmentAlreadyExistsException.class,
                () -> useCase.createDepartment(command));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar entrada inválida antes de consultar o repositório")
    void createDepartmentRejectsInvalidInputBeforeAccessingRepository() {
        // Preparação: nome em branco viola a regra do value object DepartmentName.
        CreateDepartmentCommand command = new CreateDepartmentCommand("  ", "CC-10");

        // Verificação: a regra deve falhar antes de qualquer consulta ou gravação.
        assertThrows(DomainException.class, () -> useCase.createDepartment(command));

        verify(departmentRepository, never()).existsByName(any());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar comando nulo sem acessar o repositório")
    void createDepartmentRejectsNullCommand() {
        // Verificação: comando ausente é inválido e não deve iniciar operações de persistência.
        assertThrows(NullPointerException.class, () -> useCase.createDepartment(null));

        verify(departmentRepository, never()).existsByName(any());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve propagar falha ocorrida durante a persistência")
    void createDepartmentPropagatesRepositoryFailure() {
        // Preparação: nome disponível, mas repositório falha ao salvar.
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        IllegalStateException failure = new IllegalStateException("Repository failure");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenThrow(failure);

        // Verificação: falhas de persistência não devem ser ocultadas pelo caso de uso.
        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> useCase.createDepartment(command));

        assertEquals(failure, result);
    }
}
