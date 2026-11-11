package com.banco.hr.infrastructure.adapters.inbound.rest.position;

import com.banco.hr.application.ports.inbound.CreatePositionCommand;
import com.banco.hr.application.ports.inbound.CreatePositionUseCase;
import com.banco.hr.domain.exception.PositionAlreadyExistsException;
import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;
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
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PositionControllerTest {

    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private CreatePositionUseCase createPositionUseCase;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Preparação: configura MVC isolado com validação e caso de uso substituído por mock.
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PositionController(createPositionUseCase))
                .setControllerAdvice(new PositionExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    @DisplayName("Deve criar cargo e responder 201 com recurso e Location")
    void createReturnsCreatedPositionAndLocation() throws Exception {
        // Preparação: configura o caso de uso para devolver o cargo criado.
        Position position = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);
        when(createPositionUseCase.createPosition(
                new CreatePositionCommand("Software Engineer", "Senior"))).thenReturn(position);

        // Execução e verificação: a resposta deve conter o cargo e seu URI de localização.
        mockMvc.perform(post("/api/v1/positions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Software Engineer",
                                  "level": "Senior"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/v1/positions/" + POSITION_ID)))
                .andExpect(jsonPath("$.id").value(POSITION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Software Engineer"))
                .andExpect(jsonPath("$.level").value("Senior"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-11T09:00:00-03:00"));

        // Verificação: o request deve ser convertido no comando esperado para a aplicação.
        verify(createPositionUseCase).createPosition(
                new CreatePositionCommand("Software Engineer", "Senior"));
    }

    @Test
    @DisplayName("Deve rejeitar campos obrigatórios vazios com 400 sem chamar o caso de uso")
    void createRejectsBlankRequiredFields() throws Exception {
        // Preparação: o payload viola as validações REST de título e nível.
        String invalidRequest = """
                {
                  "title": " ",
                  "level": ""
                }
                """;

        // Execução e verificação: MVC deve rejeitar o payload antes da camada de negócios.
        mockMvc.perform(post("/api/v1/positions")
                        .contentType(APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // Verificação: nenhuma entrada inválida deve ser enviada ao caso de uso.
        verify(createPositionUseCase, never()).createPosition(any(CreatePositionCommand.class));
    }

    @Test
    @DisplayName("Deve responder 409 quando já existir cargo com o mesmo título e nível")
    void createReturnsConflictWhenPositionAlreadyExists() throws Exception {
        // Preparação: o caso de uso informa que a combinação já está cadastrada.
        when(createPositionUseCase.createPosition(
                new CreatePositionCommand("Software Engineer", "Senior")))
                .thenThrow(new PositionAlreadyExistsException("Software Engineer", "Senior"));

        // Execução e verificação: a duplicidade deve ser convertida em conflito HTTP.
        mockMvc.perform(post("/api/v1/positions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Software Engineer",
                                  "level": "Senior"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cargo já cadastrado"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
