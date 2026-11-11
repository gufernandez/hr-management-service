package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import com.banco.hr.application.ports.inbound.ActivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.CreateEmployeeCommand;
import com.banco.hr.application.ports.inbound.CreateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeactivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeleteEmployeeUseCase;
import com.banco.hr.application.ports.inbound.EmployeePage;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.inbound.ListEmployeesUseCase;
import com.banco.hr.application.ports.inbound.SearchEmployeesByDepartmentUseCase;
import com.banco.hr.domain.exception.DepartmentNotFoundException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.exception.EmployeeNotFoundException;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.employee.Email;
import com.banco.hr.domain.model.employee.Employee;
import com.banco.hr.domain.model.employee.EmployeeId;
import com.banco.hr.domain.model.employee.EmployeeName;
import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.domain.model.position.PositionId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private static final UUID EMPLOYEE_ID =
            UUID.fromString("7d261066-3d41-47db-8f83-5745ac9e0ca7");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;
    @Mock
    private ListEmployeesUseCase listEmployeesUseCase;
    @Mock
    private DeleteEmployeeUseCase deleteEmployeeUseCase;
    @Mock
    private SearchEmployeesByDepartmentUseCase searchEmployeesByDepartmentUseCase;
    @Mock
    private ActivateEmployeeUseCase activateEmployeeUseCase;
    @Mock
    private DeactivateEmployeeUseCase deactivateEmployeeUseCase;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        EmployeeController controller = new EmployeeController(
                createEmployeeUseCase,
                listEmployeesUseCase,
                deleteEmployeeUseCase,
                searchEmployeesByDepartmentUseCase,
                activateEmployeeUseCase,
                deactivateEmployeeUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new EmployeeExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    @DisplayName("Deve cadastrar colaborador e responder 201 com Location e dados")
    void createReturnsCreatedEmployee() throws Exception {
        // Preparação: define o colaborador criado e o comando esperado pela aplicação.
        Employee employee = employee(EmployeeStatus.CREATED);
        when(createEmployeeUseCase.createEmployee(new CreateEmployeeCommand(
                "Ana Silva", "ana@example.com", DEPARTMENT_ID, POSITION_ID)))
                .thenReturn(employee);

        // Execução e verificação: confirma status, Location, payload e formato do timestamp.
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith(
                                "/api/v1/employees/" + EMPLOYEE_ID)))
                .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.departmentId").value(DEPARTMENT_ID.toString()))
                .andExpect(jsonPath("$.positionId").value(POSITION_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-11T09:00:00-03:00"));

        verify(createEmployeeUseCase).createEmployee(new CreateEmployeeCommand(
                "Ana Silva", "ana@example.com", DEPARTMENT_ID, POSITION_ID));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro inválido antes de chamar o caso de uso")
    void createRejectsInvalidRequest() throws Exception {
        // Execução e verificação: campos obrigatórios e e-mail inválidos devem retornar 400.
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid",
                                  "departmentId": null,
                                  "positionId": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(createEmployeeUseCase, never()).createEmployee(any(CreateEmployeeCommand.class));
    }

    @Test
    @DisplayName("Deve responder 409 quando o e-mail já estiver cadastrado")
    void createReturnsConflictWhenEmailExists() throws Exception {
        // Preparação: simula conflito informado pela camada de aplicação.
        when(createEmployeeUseCase.createEmployee(any(CreateEmployeeCommand.class)))
                .thenThrow(new EmployeeAlreadyExistsException("ana@example.com"));

        // Execução e verificação: a exceção de domínio deve ser mapeada para HTTP 409.
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Colaborador já cadastrado"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Deve responder 404 quando o departamento referenciado não existir")
    void createReturnsNotFoundWhenDepartmentDoesNotExist() throws Exception {
        // Preparação: simula referência de departamento inexistente.
        when(createEmployeeUseCase.createEmployee(any(CreateEmployeeCommand.class)))
                .thenThrow(new DepartmentNotFoundException(DEPARTMENT_ID));

        // Execução e verificação: referência ausente deve resultar em HTTP 404.
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve listar colaboradores sem paginação quando não há parâmetros")
    void listReturnsUnpagedEmployeesWhenPaginationIsOmitted() throws Exception {
        // Preparação: configura a operação sem paginação do caso de uso.
        when(listEmployeesUseCase.listEmployees()).thenReturn(List.of(employee(EmployeeStatus.CREATED)));

        // Execução e verificação: a resposta não paginada deve ser um array de colaboradores.
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EMPLOYEE_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("CREATED"));

        verify(listEmployeesUseCase).listEmployees();
    }

    @Test
    @DisplayName("Deve listar colaboradores com paginação quando offset e limit forem enviados")
    void listReturnsPagedEmployeesWhenPaginationIsRequested() throws Exception {
        // Preparação: configura uma página com metadados conhecidos.
        EmployeePage page = new EmployeePage(
                List.of(employee(EmployeeStatus.CREATED)), 5, 10, 21);
        when(listEmployeesUseCase.listEmployees(new EmployeePageRequest(5, 10))).thenReturn(page);

        // Execução e verificação: conteúdo e metadados devem ser preservados na resposta.
        mockMvc.perform(get("/api/v1/employees")
                        .param("offset", "5")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(EMPLOYEE_ID.toString()))
                .andExpect(jsonPath("$.offset").value(5))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(21));
    }

    @Test
    @DisplayName("Deve rejeitar paginação parcial ou com valores inválidos")
    void listRejectsInvalidPagination() throws Exception {
        // Verificação: paginação parcial, offset negativo e limite inválido retornam 400.
        mockMvc.perform(get("/api/v1/employees").param("offset", "5"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/employees")
                        .param("offset", "-1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve excluir logicamente colaborador e responder 204")
    void deleteReturnsNoContent() throws Exception {
        // Preparação: representa a exclusão lógica executada pelo caso de uso.
        when(deleteEmployeeUseCase.deleteEmployee(EMPLOYEE_ID))
                .thenReturn(employee(EmployeeStatus.DELETED));

        // Execução e verificação: DELETE não expõe corpo e responde 204.
        mockMvc.perform(delete("/api/v1/employees/{id}", EMPLOYEE_ID))
                .andExpect(status().isNoContent());

        verify(deleteEmployeeUseCase).deleteEmployee(EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Deve responder 404 ao excluir colaborador inexistente")
    void deleteReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        // Preparação: simula identificador não encontrado na aplicação.
        when(deleteEmployeeUseCase.deleteEmployee(EMPLOYEE_ID))
                .thenThrow(new EmployeeNotFoundException(EMPLOYEE_ID));

        // Execução e verificação: a exceção deve ser convertida para HTTP 404.
        mockMvc.perform(delete("/api/v1/employees/{id}", EMPLOYEE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve buscar colaboradores paginados pelo nome do departamento")
    void searchByDepartmentReturnsPagedEmployees() throws Exception {
        // Preparação: configura resultado paginado para o departamento solicitado.
        EmployeePage page = new EmployeePage(
                List.of(employee(EmployeeStatus.ACTIVE)), 0, 10, 1);
        when(searchEmployeesByDepartmentUseCase.searchByDepartment(
                "Engenharia", new EmployeePageRequest(0, 10))).thenReturn(page);

        // Execução e verificação: valida conteúdo e parâmetros encaminhados ao caso de uso.
        mockMvc.perform(get("/api/v1/employees/search")
                        .param("department", "Engenharia")
                        .param("offset", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.total").value(1));

        verify(searchEmployeesByDepartmentUseCase).searchByDepartment(
                "Engenharia", new EmployeePageRequest(0, 10));
    }

    @Test
    @DisplayName("Deve ativar cadastro e retornar status atualizado")
    void activateReturnsUpdatedEmployee() throws Exception {
        // Preparação: configura o agregado após a transição para ACTIVE.
        when(activateEmployeeUseCase.activateEmployee(EMPLOYEE_ID))
                .thenReturn(employee(EmployeeStatus.ACTIVE));

        // Execução e verificação: a resposta deve refletir o novo status.
        mockMvc.perform(post("/api/v1/employees/{id}/activate", EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Deve responder 400 quando a ativação violar a transição permitida")
    void activateReturnsBadRequestForInvalidTransition() throws Exception {
        // Preparação: simula rejeição da transição pela camada de domínio.
        when(activateEmployeeUseCase.activateEmployee(EMPLOYEE_ID))
                .thenThrow(new DomainException("Transição inválida."));

        // Execução e verificação: transição proibida deve ser HTTP 400.
        mockMvc.perform(post("/api/v1/employees/{id}/activate", EMPLOYEE_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve desativar cadastro e retornar status atualizado")
    void deactivateReturnsUpdatedEmployee() throws Exception {
        // Preparação: configura o agregado após a transição para INACTIVE.
        when(deactivateEmployeeUseCase.deactivateEmployee(EMPLOYEE_ID))
                .thenReturn(employee(EmployeeStatus.INACTIVE));

        // Execução e verificação: a resposta deve refletir o novo status.
        mockMvc.perform(post("/api/v1/employees/{id}/deactivate", EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private static String createRequest() {
        return """
                {
                  "name": "Ana Silva",
                  "email": "ana@example.com",
                  "departmentId": "%s",
                  "positionId": "%s"
                }
                """.formatted(DEPARTMENT_ID, POSITION_ID);
    }

    private static Employee employee(EmployeeStatus status) {
        Instant updatedAt = status == EmployeeStatus.CREATED
                ? CREATED_AT
                : CREATED_AT.plusSeconds(60);
        Instant deletedAt = status == EmployeeStatus.DELETED ? updatedAt : null;
        return Employee.reconstitute(
                new EmployeeId(EMPLOYEE_ID),
                new EmployeeName("Ana Silva"),
                new Email("ana@example.com"),
                new DepartmentId(DEPARTMENT_ID),
                new PositionId(POSITION_ID),
                status,
                CREATED_AT,
                updatedAt,
                deletedAt);
    }
}
