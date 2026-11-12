package com.banco.hr.infrastructure.adapters.inbound.rest.department;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.banco.hr.application.ports.inbound.CreateDepartmentUseCase;
import com.banco.hr.application.ports.inbound.dto.CreateDepartmentCommand;
import com.banco.hr.domain.exception.DepartmentAlreadyExistsException;
import com.banco.hr.domain.model.department.CostCenterCode;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.domain.model.department.DepartmentId;
import com.banco.hr.domain.model.department.DepartmentName;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private CreateDepartmentUseCase createDepartmentUseCase;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        // Preparação: configura MVC isolado com validação e caso de uso substituído por mock.
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DepartmentController(createDepartmentUseCase))
                .setControllerAdvice(new DepartmentExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    @DisplayName("Deve criar departamento e responder 201 com recurso e Location")
    void createReturnsCreatedDepartmentAndLocation() throws Exception {
        // Preparação: configura o caso de uso para devolver o departamento criado.
        Department department = Department.reconstitute(
                new DepartmentId(DEPARTMENT_ID),
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);
        when(createDepartmentUseCase.createDepartment(
                new CreateDepartmentCommand("People", "CC-10"))).thenReturn(department);

        // Execução e verificação: a resposta deve conter o recurso criado e seu URI de localização.
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "People",
                                  "costCenterCode": "CC-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/v1/departments/" + DEPARTMENT_ID)))
                .andExpect(jsonPath("$.id").value(DEPARTMENT_ID.toString()))
                .andExpect(jsonPath("$.name").value("People"))
                .andExpect(jsonPath("$.costCenterCode").value("CC-10"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-11T09:00:00-03:00"));

        // Verificação: o request deve ser convertido no comando esperado para a aplicação.
        verify(createDepartmentUseCase).createDepartment(
                new CreateDepartmentCommand("People", "CC-10"));
    }

    @Test
    @DisplayName("Deve rejeitar campos obrigatórios vazios com 400 sem chamar o caso de uso")
    void createRejectsBlankRequiredFields() throws Exception {
        // Preparação: o payload viola as regras de validação do request REST.
        String invalidRequest = """
                {
                  "name": " ",
                  "costCenterCode": ""
                }
                """;

        // Execução e verificação: MVC deve rejeitar o payload antes da camada de negócios.
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // Verificação: nenhum comando deve ser enviado ao caso de uso para entrada inválida.
        verify(createDepartmentUseCase, never()).createDepartment(
                any(CreateDepartmentCommand.class));
    }

    @Test
    @DisplayName("Deve responder 409 quando o nome do departamento já estiver cadastrado")
    void createReturnsConflictWhenDepartmentNameAlreadyExists() throws Exception {
        // Preparação: o caso de uso comunica que o nome solicitado já está cadastrado.
        when(createDepartmentUseCase.createDepartment(
                new CreateDepartmentCommand("People", "CC-10")))
                .thenThrow(new DepartmentAlreadyExistsException("People"));

        // Execução e verificação: a exceção de domínio deve ser traduzida para conflito HTTP.
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "People",
                                  "costCenterCode": "CC-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Departamento já cadastrado"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
