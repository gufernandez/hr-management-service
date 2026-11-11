package com.banco.hr.infrastructure.adapters.inbound.rest.department;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.DepartmentJpaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DepartmentControllerIntegrationTest {

    private static final String DEPARTMENTS_ENDPOINT = "/api/v1/departments";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DepartmentJpaRepository departmentRepository;

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
    @SuppressWarnings("unused")
    void cleanDatabase() {
        // Preparação: cada cenário começa sem departamentos para isolar as verificações de estado.
        departmentRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar departamento via HTTP e persistir todos os dados no PostgreSQL")
    void createDepartmentPersistsDataAndReturnsCreatedResponse() {
        // Preparação: confirma que o contexto usa a instância PostgreSQL inicializada pelo Testcontainers.
        assertEquals(POSTGRES.getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        CreateDepartmentRequest request = new CreateDepartmentRequest("People", "CC-10");

        // Execução: envia uma requisição HTTP real ao endpoint de criação.
        ResponseEntity<DepartmentResponse> response =
                restTemplate.postForEntity(DEPARTMENTS_ENDPOINT, request, DepartmentResponse.class);

        // Verificação: o endpoint deve retornar o recurso e um Location com o identificador gerado.
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        DepartmentResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("People", body.name());
        assertEquals("CC-10", body.costCenterCode());
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith(body.id().toString()));

        // Verificação: o timestamp da resposta é ISO 8601 e mantém o offset do horário de Brasília.
        assertEquals(ZoneOffset.ofHours(-3), OffsetDateTime.parse(body.createdAt()).getOffset());

        // Verificação: os mesmos campos devem ter sido gravados na tabela department.
        DepartmentJpaEntity persistedDepartment = departmentRepository.findById(body.id())
                .orElseThrow(() -> new AssertionError("Departamento não encontrado no banco."));
        assertEquals(body.id(), persistedDepartment.getId());
        assertEquals("People", persistedDepartment.getName());
        assertEquals("CC-10", persistedDepartment.getCostCenterCode());
        assertNotNull(persistedDepartment.getCreatedAt());
        assertEquals(1, departmentRepository.count());
    }

    @Test
    @DisplayName("Deve responder conflito e manter um único registro para nome duplicado")
    void createDepartmentRejectsDuplicateNameAndKeepsSingleDatabaseRecord() {
        // Preparação: cria o primeiro departamento para que o nome já exista no banco.
        CreateDepartmentRequest request = new CreateDepartmentRequest("People", "CC-10");
        ResponseEntity<DepartmentResponse> firstResponse =
                restTemplate.postForEntity(DEPARTMENTS_ENDPOINT, request, DepartmentResponse.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        // Execução: envia uma segunda requisição com o mesmo nome.
        ResponseEntity<String> duplicateResponse =
                restTemplate.exchange(
                        DEPARTMENTS_ENDPOINT,
                        HttpMethod.POST,
                        jsonRequest(request),
                        String.class);

        // Verificação: a duplicidade deve ser mapeada para HTTP 409 sem inserir outra linha.
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        assertEquals(1, departmentRepository.count());
        assertTrue(departmentRepository.existsByName("People"));
    }

    @Test
    @DisplayName("Deve responder erro de validação sem persistir campos obrigatórios vazios")
    void createDepartmentRejectsBlankFieldsWithoutPersisting() {
        // Preparação: monta uma requisição com nome e código do centro de custo em branco.
        CreateDepartmentRequest request = new CreateDepartmentRequest(" ", "");

        // Execução: envia a requisição inválida ao endpoint HTTP.
        ResponseEntity<String> response =
                restTemplate.exchange(
                        DEPARTMENTS_ENDPOINT,
                        HttpMethod.POST,
                        jsonRequest(request),
                        String.class);

        // Verificação: validação REST deve retornar 400 e deixar a tabela sem registros.
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(0, departmentRepository.count());
    }

    private HttpEntity<CreateDepartmentRequest> jsonRequest(CreateDepartmentRequest request) {
        // Preparação: define explicitamente JSON para que o servidor use o mapeamento REST esperado.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}
