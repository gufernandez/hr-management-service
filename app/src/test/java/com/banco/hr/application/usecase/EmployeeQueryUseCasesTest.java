package com.banco.hr.application.usecase;

import com.banco.hr.application.ports.inbound.dto.PageQuery;
import com.banco.hr.application.ports.outbound.DepartmentLookupPort;
import com.banco.hr.application.ports.outbound.EmployeeRepositoryPort;
import com.banco.hr.domain.exception.DepartmentNameNotFoundException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeePage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeQueryUseCasesTest {

    private static final DepartmentId DEPARTMENT_ID =
            new DepartmentId(UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397"));
    private static final PageQuery PAGE_REQUEST = new PageQuery(20, 10);

    @Mock
    private EmployeeRepositoryPort employeeRepository;

    @Mock
    private DepartmentLookupPort departmentLookup;

    private ListEmployeesUseCaseImpl listUseCase;
    private SearchEmployeesByDepartmentUseCaseImpl searchUseCase;

    @BeforeEach
    void setUp() {
        // Preparação: inicializa os casos de uso com as portas de saída simuladas.
        listUseCase = new ListEmployeesUseCaseImpl(employeeRepository);
        searchUseCase =
                new SearchEmployeesByDepartmentUseCaseImpl(departmentLookup, employeeRepository);
    }

    @Test
    @DisplayName("Deve delegar listagem paginada excluindo colaboradores deletados")
    void listEmployeesDelegatesPageRequestToRepository() {
        // Preparação: configura a página que o repositório retorna.
        EmployeePage expected = new EmployeePage(List.of(), 20, 10, 0);
        when(employeeRepository.findAllNotDeleted(PAGE_REQUEST)).thenReturn(expected);

        // Execução: lista a página solicitada.
        EmployeePage result = listUseCase.listEmployees(PAGE_REQUEST);

        // Verificação: o caso de uso retorna exatamente a página obtida do repositório.
        assertSame(expected, result);
        verify(employeeRepository).findAllNotDeleted(PAGE_REQUEST);
    }

    @Test
    @DisplayName("Deve listar todos os colaboradores não deletados sem paginação")
    void listEmployeesWithoutPageRequestReturnsAllNotDeletedEmployees() {
        // Preparação: configura lista sem limite nem offset fornecido pelo consumidor.
        List<Employee> expected = List.of();
        when(employeeRepository.findAllNotDeleted()).thenReturn(expected);

        // Execução: lista colaboradores omitindo os parâmetros opcionais de paginação.
        List<Employee> result = listUseCase.listEmployees();

        // Verificação: a lista não paginada é devolvida e o repositório é chamado sem página.
        assertEquals(expected, result);
        verify(employeeRepository).findAllNotDeleted();
    }

    @Test
    @DisplayName("Deve buscar colaboradores paginados pelo nome do departamento")
    void searchByDepartmentResolvesNameAndQueriesEmployeesByDepartmentId() {
        // Preparação: resolve o nome para o UUID do departamento e configura o resultado.
        DepartmentName departmentName = new DepartmentName("People");
        EmployeePage expected = new EmployeePage(List.of(), 20, 10, 0);
        when(departmentLookup.findIdByName(departmentName)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(employeeRepository.findNotDeletedByDepartment(DEPARTMENT_ID, PAGE_REQUEST))
                .thenReturn(expected);

        // Execução: busca os colaboradores usando o nome recebido e a página solicitada.
        EmployeePage result = searchUseCase.searchByDepartment("People", PAGE_REQUEST);

        // Verificação: consulta por UUID evita acoplamento do employee repository ao nome.
        assertSame(expected, result);
        verify(departmentLookup).findIdByName(departmentName);
        verify(employeeRepository).findNotDeletedByDepartment(DEPARTMENT_ID, PAGE_REQUEST);
    }

    @Test
    @DisplayName("Deve buscar todos os colaboradores não deletados sem paginação por departamento")
    void searchByDepartmentWithoutPaginationReturnsAllNotDeletedEmployees() {
        // Preparação: resolve o nome do departamento e configura uma lista completa.
        DepartmentName departmentName = new DepartmentName("People");
        List<Employee> expected = List.of();
        when(departmentLookup.findIdByName(departmentName)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(employeeRepository.findNotDeletedByDepartment(DEPARTMENT_ID)).thenReturn(expected);

        // Execução: busca os colaboradores omitindo os parâmetros de paginação.
        List<Employee> result = searchUseCase.searchByDepartment("People");

        // Verificação: o caso de uso retorna a lista consultada pelo ID resolvido.
        assertEquals(expected, result);
        verify(departmentLookup).findIdByName(departmentName);
        verify(employeeRepository).findNotDeletedByDepartment(DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Deve rejeitar departamento inexistente sem consultar colaboradores")
    void searchByDepartmentRejectsUnknownName() {
        // Preparação: o nome solicitado não está cadastrado.
        DepartmentName departmentName = new DepartmentName("Unknown");
        when(departmentLookup.findIdByName(departmentName)).thenReturn(Optional.empty());

        // Verificação: uma referência inexistente produz exceção sem consultar a listagem.
        assertThrows(
                DepartmentNameNotFoundException.class,
                () -> searchUseCase.searchByDepartment("Unknown", PAGE_REQUEST));
        verify(employeeRepository, never())
                .findNotDeletedByDepartment(DEPARTMENT_ID, PAGE_REQUEST);
    }

    @Test
    @DisplayName("Deve rejeitar nome de departamento vazio")
    void searchByDepartmentRejectsBlankName() {
        // Verificação: a regra do value object impede lookup para nome vazio.
        assertThrows(
                DomainException.class,
                () -> searchUseCase.searchByDepartment(" ", PAGE_REQUEST));
        verify(departmentLookup, never()).findIdByName(any());
    }
}
