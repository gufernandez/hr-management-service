package com.banco.hr.infrastructure.adapters.inbound.rest.position;

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.PositionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PositionControllerIntegrationTest {

    private static final String POSITIONS_ENDPOINT = "/api/v1/positions";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    @SuppressWarnings("unused") // Invoked by Spring TestContext through @DynamicPropertySource.
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        // Preparação: cada cenário começa sem cargos para isolar as verificações de estado.
        positionRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar cargo via HTTP e persistir todos os dados no PostgreSQL")
    void createPositionPersistsDataAndReturnsCreatedResponse() {
        // Preparação: confirma que o contexto usa a instância PostgreSQL inicializada pelo Testcontainers.
        assertEquals(POSTGRES.getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        CreatePositionRequest request =
                new CreatePositionRequest("Software Engineer", "Senior");

        // Execução: envia uma requisição HTTP real ao endpoint de criação.
        ResponseEntity<PositionResponse> response =
                restTemplate.postForEntity(POSITIONS_ENDPOINT, request, PositionResponse.class);

        // Verificação: o endpoint deve retornar o recurso e um Location com o identificador gerado.
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PositionResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("Software Engineer", body.title());
        assertEquals("Senior", body.level());
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith(body.id().toString()));

        // Verificação: o timestamp da resposta mantém o offset do horário de Brasília.
        assertEquals(ZoneOffset.ofHours(-3), OffsetDateTime.parse(body.createdAt()).getOffset());

        // Verificação: os mesmos campos devem ter sido gravados na tabela position.
        PositionJpaEntity persistedPosition = positionRepository.findById(body.id())
                .orElseThrow(() -> new AssertionError("Cargo não encontrado no banco."));
        assertEquals(body.id(), persistedPosition.getId());
        assertEquals("Software Engineer", persistedPosition.getTitle());
        assertEquals("Senior", persistedPosition.getLevel());
        assertNotNull(persistedPosition.getCreatedAt());
        assertEquals(1, positionRepository.count());
    }

    @Test
    @DisplayName("Deve responder erro de validação sem persistir campos obrigatórios vazios")
    void createPositionRejectsBlankFieldsWithoutPersisting() {
        // Preparação: envia título e nível em branco, que são inválidos no contrato da API.
        CreatePositionRequest request = new CreatePositionRequest(" ", "");

        // Execução: envia a requisição inválida ao endpoint HTTP.
        ResponseEntity<String> response =
                restTemplate.exchange(
                        POSITIONS_ENDPOINT,
                        HttpMethod.POST,
                        jsonRequest(request),
                        String.class);

        // Verificação: a validação deve responder 400 e deixar a tabela sem registros.
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(0, positionRepository.count());
    }

    @Test
    @DisplayName("Deve responder conflito e não duplicar cargo com mesmo título e nível")
    void createPositionRejectsDuplicateTitleAndLevelWithoutPersistingAnotherPosition() {
        // Preparação: cria o primeiro cargo e confirma sua forma normalizada no banco.
        CreatePositionRequest request =
                new CreatePositionRequest("engenheiro  de software", "junior");
        ResponseEntity<PositionResponse> firstResponse =
                restTemplate.postForEntity(POSITIONS_ENDPOINT, request, PositionResponse.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        assertEquals("Engenheiro de Software", firstResponse.getBody().title());
        assertEquals("Junior", firstResponse.getBody().level());

        // Execução: envia a mesma combinação em outra caixa e com espaçamento diferente.
        CreatePositionRequest duplicateRequest =
                new CreatePositionRequest("ENGENHEIRO de SOFTWARE", "JUNIOR");
        ResponseEntity<String> duplicateResponse =
                restTemplate.exchange(
                        POSITIONS_ENDPOINT,
                        HttpMethod.POST,
                        jsonRequest(duplicateRequest),
                        String.class);

        // Verificação: a API informa conflito e mantém somente o primeiro registro.
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        assertEquals(1, positionRepository.count());
        PositionJpaEntity persistedPosition = positionRepository.findAll().get(0);
        assertEquals(firstResponse.getBody().id(), persistedPosition.getId());
        assertEquals("Engenheiro de Software", persistedPosition.getTitle());
        assertEquals("Junior", persistedPosition.getLevel());
    }

    @Test
    @DisplayName("Deve permitir mesmo título ou nível quando o par for diferente")
    void createPositionAllowsSameTitleOrLevelWhenThePairIsDifferent() {
        // Preparação: cria o cargo inicial e variações que diferem em um dos dois campos.
        CreatePositionRequest original = new CreatePositionRequest("Software Engineer", "Senior");
        CreatePositionRequest differentLevel =
                new CreatePositionRequest("Software Engineer", "Junior");
        CreatePositionRequest differentTitle =
                new CreatePositionRequest("Engineering Manager", "Senior");

        // Execução: envia as três combinações pelo endpoint HTTP.
        ResponseEntity<PositionResponse> originalResponse =
                restTemplate.postForEntity(POSITIONS_ENDPOINT, original, PositionResponse.class);
        ResponseEntity<PositionResponse> differentLevelResponse =
                restTemplate.postForEntity(
                        POSITIONS_ENDPOINT, differentLevel, PositionResponse.class);
        ResponseEntity<PositionResponse> differentTitleResponse =
                restTemplate.postForEntity(
                        POSITIONS_ENDPOINT, differentTitle, PositionResponse.class);

        // Verificação: somente a repetição do par completo é inválida.
        assertEquals(HttpStatus.CREATED, originalResponse.getStatusCode());
        assertEquals(HttpStatus.CREATED, differentLevelResponse.getStatusCode());
        assertEquals(HttpStatus.CREATED, differentTitleResponse.getStatusCode());
        assertEquals(3, positionRepository.count());
    }

    private HttpEntity<CreatePositionRequest> jsonRequest(CreatePositionRequest request) {
        // Preparação: define JSON para exercitar o mapeamento REST do request.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}
