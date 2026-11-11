package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.CreateDepartmentCommand;
import com.banco.hr.application.ports.outbound.DepartmentRepositoryPort;
import com.banco.hr.domain.exception.DepartmentAlreadyExistsException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentName;
import org.junit.jupiter.api.BeforeEach;
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
        useCase = new CreateDepartmentUseCaseImpl(
                departmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createDepartmentSavesAndReturnsDepartmentWithGeneratedIdAndTimestamp() {
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Department result = useCase.createDepartment(command);

        assertNotNull(result.id().value());
        assertEquals("People", result.name().value());
        assertEquals("CC-10", result.costCenterCode().value());
        assertEquals(NOW, result.createdAt());
        verify(departmentRepository).existsByName(new DepartmentName("People"));
        verify(departmentRepository).save(result);
    }

    @Test
    void createDepartmentTrimsValuesBeforeCheckingAndSaving() {
        CreateDepartmentCommand command =
                new CreateDepartmentCommand("  People  ", "  CC-10  ");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Department result = useCase.createDepartment(command);

        assertEquals("People", result.name().value());
        assertEquals("CC-10", result.costCenterCode().value());
        verify(departmentRepository).existsByName(new DepartmentName("People"));
    }

    @Test
    void createDepartmentRejectsDuplicateNameWithoutSaving() {
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(true);

        assertThrows(DepartmentAlreadyExistsException.class,
                () -> useCase.createDepartment(command));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartmentRejectsInvalidInputBeforeAccessingRepository() {
        CreateDepartmentCommand command = new CreateDepartmentCommand("  ", "CC-10");

        assertThrows(DomainException.class, () -> useCase.createDepartment(command));

        verify(departmentRepository, never()).existsByName(any());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartmentRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> useCase.createDepartment(null));

        verify(departmentRepository, never()).existsByName(any());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartmentPropagatesRepositoryFailure() {
        CreateDepartmentCommand command = new CreateDepartmentCommand("People", "CC-10");
        IllegalStateException failure = new IllegalStateException("Repository failure");
        when(departmentRepository.existsByName(new DepartmentName("People"))).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenThrow(failure);

        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> useCase.createDepartment(command));

        assertEquals(failure, result);
    }
}
