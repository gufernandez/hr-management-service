package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import com.banco.hr.domain.model.employee.EmployeeStatus;
import com.banco.hr.infrastructure.adapters.inbound.rest.employee.dto.CreateEmployeeRequest;
import com.banco.hr.infrastructure.adapters.inbound.rest.employee.dto.EmployeePageResponse;
import com.banco.hr.infrastructure.adapters.inbound.rest.employee.dto.EmployeeResponse;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.DepartmentJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.EmployeeJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.OutboxEventJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.DepartmentJpaRepository;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.EmployeeJpaRepository;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.OutboxEventJpaRepository;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EmployeeControllerIntegrationTest {

    private static final String EMPLOYEES_ENDPOINT = "/api/v1/employees";
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("c1e49b85-7a82-4c50-a1b1-548c8c0eb397");
    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final UUID UNKNOWN_UUID =
            UUID.fromString("47f18d06-35b8-4ac4-a01e-c993a31f32be");
    private static final Instant REFERENCE_TIME = Instant.parse("2026-11-11T12:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DepartmentJpaRepository departmentRepository;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private EmployeeJpaRepository employeeRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void prepareDatabase() {
        outboxEventRepository.deleteAll();
        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        departmentRepository.save(new DepartmentJpaEntity(
                DEPARTMENT_ID, "Engineering", "COST-100", REFERENCE_TIME));
        positionRepository.save(new PositionJpaEntity(
                POSITION_ID, "Software Engineer", "Senior", REFERENCE_TIME));
    }

    @Test
    @DisplayName("Deve cadastrar colaborador por HTTP e persistir a resposta no PostgreSQL")
    void createEmployeePersistsDataAndReturnsCreatedResponse() {
        // Preparação: confirma que a aplicação utiliza o PostgreSQL isolado do Testcontainers.
        assertEquals(POSTGRES.getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        CreateEmployeeRequest request = request("Ana Silva", "ana@example.com");

        // Execução: envia um request HTTP real para o endpoint de cadastro.
        ResponseEntity<EmployeeResponse> response =
                restTemplate.postForEntity(EMPLOYEES_ENDPOINT, request, EmployeeResponse.class);

        // Verificação: valida status, identificadores, estado inicial e formato da resposta.
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        EmployeeResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("Ana Silva", body.name());
        assertEquals("ana@example.com", body.email());
        assertEquals(DEPARTMENT_ID, body.departmentId());
        assertEquals(POSITION_ID, body.positionId());
        assertEquals("CREATED", body.status());
        assertNull(body.deletedAt());
        assertEquals(ZoneOffset.ofHours(-3), OffsetDateTime.parse(body.createdAt()).getOffset());
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith(body.id().toString()));

        // Verificação: todos os dados de negócio devem estar gravados na tabela employee.
        EmployeeJpaEntity persisted = employeeRepository.findById(body.id())
                .orElseThrow(() -> new AssertionError("Colaborador não encontrado no banco."));
        assertEquals(body.id(), persisted.getId());
        assertEquals("Ana Silva", persisted.getName());
        assertEquals("ana@example.com", persisted.getEmail());
        assertEquals(DEPARTMENT_ID, persisted.getDepartmentId());
        assertEquals(POSITION_ID, persisted.getPositionId());
        assertEquals(EmployeeStatus.CREATED, persisted.getStatus());
        assertNotNull(persisted.getCreatedAt());
        assertEquals(persisted.getCreatedAt(), persisted.getLastUpdatedAt());
        assertNull(persisted.getDeletedAt());
        assertEquals(1, employeeRepository.count());

        // Verificação: o cadastro grava atomicamente o evento de criação na outbox.
        OutboxEventJpaEntity outboxEvent = outboxEventRepository.findAll().get(0);
        assertEquals(1, outboxEventRepository.count());
        assertNotNull(outboxEvent.getEventId());
        assertEquals(body.id(), outboxEvent.getEmployeeId());
        assertEquals(EmployeeStatus.CREATED, outboxEvent.getEmployeeStatus());
        assertEquals("EmployeeCreatedEvent", outboxEvent.getEventType());
        assertEquals("PENDING", outboxEvent.getProcessingStatus().name());
        assertEquals(0, outboxEvent.getRetryCount());
        assertNull(outboxEvent.getPublishedAt());
        assertEquals(outboxEvent.getCreatedAt(), outboxEvent.getUpdateTime());
    }

    @Test
    @DisplayName("Deve rejeitar request inválido e referências inexistentes sem persistir")
    void createEmployeeRejectsInvalidRequestAndUnknownReferences() {
        // Preparação: monta requests inválido e com departamento inexistente.
        ResponseEntity<String> invalidRequest = restTemplate.exchange(
                EMPLOYEES_ENDPOINT,
                HttpMethod.POST,
                jsonRequest("""
                        {
                          "name": "",
                          "email": "invalid-email",
                          "departmentId": "%s",
                          "positionId": "%s"
                        }
                        """.formatted(DEPARTMENT_ID, POSITION_ID)),
                String.class);
        ResponseEntity<String> missingDepartment = restTemplate.exchange(
                EMPLOYEES_ENDPOINT,
                HttpMethod.POST,
                jsonRequest("""
                        {
                          "name": "Ana Silva",
                          "email": "ana@example.com",
                          "departmentId": "%s",
                          "positionId": "%s"
                        }
                        """.formatted(UNKNOWN_UUID, POSITION_ID)),
                String.class);
        ResponseEntity<String> missingPosition = restTemplate.exchange(
                EMPLOYEES_ENDPOINT,
                HttpMethod.POST,
                jsonRequest("""
                        {
                          "name": "Ana Silva",
                          "email": "ana@example.com",
                          "departmentId": "%s",
                          "positionId": "%s"
                        }
                        """.formatted(DEPARTMENT_ID, UNKNOWN_UUID)),
                String.class);

        // Verificação: falhas de validação e de referência não devem criar registros.
        assertEquals(HttpStatus.BAD_REQUEST, invalidRequest.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, missingDepartment.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, missingPosition.getStatusCode());
        assertEquals(0, employeeRepository.count());
        assertEquals(0, outboxEventRepository.count());
    }

    @Test
    @DisplayName("Deve responder conflito para e-mail duplicado e manter um único registro")
    void createEmployeeRejectsDuplicateEmail() {
        // Preparação: cadastra primeiro colaborador usando caixa alta no e-mail.
        ResponseEntity<EmployeeResponse> first = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT,
                request("Ana Silva", "ANA@example.com"),
                EmployeeResponse.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        // Execução: envia outro cadastro com o mesmo endereço normalizado.
        ResponseEntity<String> duplicate = restTemplate.exchange(
                EMPLOYEES_ENDPOINT,
                HttpMethod.POST,
                jsonRequest("""
                        {
                          "name": "Outra Ana",
                          "email": "ana@example.com",
                          "departmentId": "%s",
                          "positionId": "%s"
                        }
                        """.formatted(DEPARTMENT_ID, POSITION_ID)),
                String.class);

        // Verificação: deve retornar conflito e preservar o registro original.
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(1, employeeRepository.count());
        assertEquals(1, outboxEventRepository.count());
        assertTrue(employeeRepository.existsByEmail("ana@example.com"));
    }

    @Test
    @DisplayName("Deve listar colaboradores com e sem paginação e excluir os deletados")
    void listEmployeesSupportsOptionalPaginationAndExcludesDeleted() {
        // Preparação: cadastra três colaboradores para exercitar listagem e paginação.
        EmployeeResponse first = createEmployee("Ana Silva", "ana@example.com");
        EmployeeResponse second = createEmployee("Bruno Souza", "bruno@example.com");
        EmployeeResponse third = createEmployee("Carla Lima", "carla@example.com");

        // Execução: obtém a listagem completa e duas páginas consecutivas.
        ResponseEntity<EmployeeResponse[]> unpaged =
                restTemplate.getForEntity(EMPLOYEES_ENDPOINT, EmployeeResponse[].class);
        ResponseEntity<EmployeePageResponse> pageOne = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "?offset=0&limit=2", EmployeePageResponse.class);
        ResponseEntity<EmployeePageResponse> pageTwo = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "?offset=2&limit=2", EmployeePageResponse.class);

        // Verificação: listagem completa e páginas refletem o conjunto persistido.
        assertEquals(HttpStatus.OK, unpaged.getStatusCode());
        assertEquals(3, unpaged.getBody().length);
        assertEquals(2, pageOne.getBody().content().size());
        assertEquals(1, pageTwo.getBody().content().size());
        assertEquals(3, pageOne.getBody().total());
        assertEquals(3, pageTwo.getBody().total());
        Set<UUID> listedIds = Set.of(
                unpaged.getBody()[0].id(),
                unpaged.getBody()[1].id(),
                unpaged.getBody()[2].id());
        assertEquals(Set.of(first.id(), second.id(), third.id()), listedIds);

        // Execução: remove logicamente um colaborador e consulta novamente a listagem.
        ResponseEntity<Void> deleted = restTemplate.exchange(
                EMPLOYEES_ENDPOINT + "/" + third.id(), HttpMethod.DELETE, null, Void.class);
        ResponseEntity<EmployeeResponse[]> afterDelete =
                restTemplate.getForEntity(EMPLOYEES_ENDPOINT, EmployeeResponse[].class);

        // Verificação: DELETE retorna 204, persiste DELETED e omite o colaborador da listagem.
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        assertEquals(2, afterDelete.getBody().length);
        EmployeeJpaEntity persisted = employeeRepository.findById(third.id()).orElseThrow();
        assertEquals(EmployeeStatus.DELETED, persisted.getStatus());
        assertNotNull(persisted.getDeletedAt());
        assertFalse(java.util.Arrays.stream(afterDelete.getBody())
                .anyMatch(employee -> employee.id().equals(third.id())));
    }

    @Test
    @DisplayName("Deve responder erro para parâmetros inválidos e colaborador inexistente")
    void listAndDeleteReturnErrorsForInvalidRequests() {
        // Execução: solicita paginação inválida e exclusão de identificador desconhecido.
        ResponseEntity<String> invalidPagination = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "?offset=-1&limit=0", String.class);
        ResponseEntity<String> partialPagination = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "?offset=1", String.class);
        ResponseEntity<String> missingEmployee = restTemplate.exchange(
                EMPLOYEES_ENDPOINT + "/" + UNKNOWN_UUID, HttpMethod.DELETE, null, String.class);

        // Verificação: entradas inválidas retornam 400, recurso ausente retorna 404.
        assertEquals(HttpStatus.BAD_REQUEST, invalidPagination.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, partialPagination.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, missingEmployee.getStatusCode());
        assertEquals(0, employeeRepository.count());
    }

    @Test
    @DisplayName("Deve buscar colaboradores paginados por departamento e persistir exclusão lógica")
    void searchEmployeesByDepartmentReturnsPagedResults() {
        // Preparação: cadastra dois colaboradores associados ao departamento existente.
        EmployeeResponse first = createEmployee("Ana Silva", "ana@example.com");
        EmployeeResponse second = createEmployee("Bruno Souza", "bruno@example.com");

        // Execução: busca uma página de colaboradores pelo nome do departamento.
        ResponseEntity<EmployeePageResponse> response = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "/search?department=Engineering&offset=0&limit=1",
                EmployeePageResponse.class);
        ResponseEntity<EmployeeResponse[]> unpagedResponse = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "/search?department=Engineering",
                EmployeeResponse[].class);

        // Verificação: busca paginada e não paginada retornam apenas os colaboradores do departamento.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().content().size());
        assertEquals(0, response.getBody().offset());
        assertEquals(1, response.getBody().limit());
        assertEquals(2, response.getBody().total());
        assertTrue(Set.of(first.id(), second.id())
                .contains(response.getBody().content().get(0).id()));
        assertEquals(HttpStatus.OK, unpagedResponse.getStatusCode());
        assertEquals(2, unpagedResponse.getBody().length);
        assertEquals(Set.of(first.id(), second.id()),
                Set.of(unpagedResponse.getBody()[0].id(), unpagedResponse.getBody()[1].id()));

        // Execução: solicita departamento inexistente, paginação inválida ou parcial.
        ResponseEntity<String> unknownDepartment = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "/search?department=Unknown&offset=0&limit=10",
                String.class);
        ResponseEntity<String> invalidPagination = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "/search?department=Engineering&offset=-1&limit=0",
                String.class);
        ResponseEntity<String> partialPagination = restTemplate.getForEntity(
                EMPLOYEES_ENDPOINT + "/search?department=Engineering&limit=10",
                String.class);

        // Verificação: inexistência retorna 404; paginação inválida ou parcial retorna 400.
        assertEquals(HttpStatus.NOT_FOUND, unknownDepartment.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidPagination.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, partialPagination.getStatusCode());
        assertEquals(2, employeeRepository.count());
    }

    @Test
    @DisplayName("Deve ativar cadastro permitido e persistir o estado ACTIVE")
    void activateEmployeePersistsActiveStatus() {
        // Preparação: cadastra colaborador no status inicial CREATED.
        EmployeeResponse created = createEmployee("Ana Silva", "ana@example.com");

        // Execução: ativa o cadastro e envia outra ativação após estado ACTIVE.
        ResponseEntity<EmployeeResponse> activated = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/activate", null,
                EmployeeResponse.class);

        // Verificação: resposta e banco devem refletir ACTIVE.
        assertEquals(HttpStatus.OK, activated.getStatusCode());
        assertEquals("ACTIVE", activated.getBody().status());
        assertEquals(EmployeeStatus.ACTIVE,
                employeeRepository.findById(created.id()).orElseThrow().getStatus());

        // Execução: desativa e ativa novamente para cobrir a transição INACTIVE -> ACTIVE.
        restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/deactivate", null,
                EmployeeResponse.class);
        ResponseEntity<EmployeeResponse> reactivated = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/activate", null,
                EmployeeResponse.class);

        // Verificação: cadastro INACTIVE pode ser ativado e volta a ACTIVE no banco.
        assertEquals(HttpStatus.OK, reactivated.getStatusCode());
        assertEquals("ACTIVE", reactivated.getBody().status());
        assertEquals(EmployeeStatus.ACTIVE,
                employeeRepository.findById(created.id()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Deve desativar cadastro permitido e persistir o estado INACTIVE")
    void deactivateEmployeePersistsInactiveStatus() {
        // Preparação: cadastra colaborador em CREATED e cria outro para ativar antes da transição.
        EmployeeResponse created = createEmployee("Ana Silva", "ana@example.com");
        EmployeeResponse active = createEmployee("Bruno Souza", "bruno@example.com");
        restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + active.id() + "/activate", null,
                EmployeeResponse.class);

        // Execução: desativa ambos os estados permitidos, CREATED e ACTIVE.
        ResponseEntity<EmployeeResponse> deactivatedCreated = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/deactivate", null,
                EmployeeResponse.class);
        ResponseEntity<EmployeeResponse> deactivatedActive = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + active.id() + "/deactivate", null,
                EmployeeResponse.class);

        // Verificação: respostas e registros persistidos devem apresentar INACTIVE.
        assertEquals(HttpStatus.OK, deactivatedCreated.getStatusCode());
        assertEquals(HttpStatus.OK, deactivatedActive.getStatusCode());
        assertEquals("INACTIVE", deactivatedCreated.getBody().status());
        assertEquals("INACTIVE", deactivatedActive.getBody().status());
        assertEquals(EmployeeStatus.INACTIVE,
                employeeRepository.findById(created.id()).orElseThrow().getStatus());
        assertEquals(EmployeeStatus.INACTIVE,
                employeeRepository.findById(active.id()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Deve rejeitar transições de status inválidas sem alterar persistência")
    void activateDeactivateAndDeleteRejectInvalidTransitions() {
        // Preparação: exclui logicamente colaborador para colocá-lo em estado terminal.
        EmployeeResponse created = createEmployee("Ana Silva", "ana@example.com");
        restTemplate.exchange(
                EMPLOYEES_ENDPOINT + "/" + created.id(), HttpMethod.DELETE, null, Void.class);

        // Execução: tenta ativar, desativar e excluir novamente o colaborador já deletado.
        ResponseEntity<String> activate = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/activate", null, String.class);
        ResponseEntity<String> deactivate = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/deactivate", null, String.class);
        ResponseEntity<String> deleteAgain = restTemplate.exchange(
                EMPLOYEES_ENDPOINT + "/" + created.id(), HttpMethod.DELETE, null, String.class);

        // Verificação: transições inválidas retornam 400 e o registro permanece DELETED.
        assertEquals(HttpStatus.BAD_REQUEST, activate.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, deactivate.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, deleteAgain.getStatusCode());
        EmployeeJpaEntity persisted = employeeRepository.findById(created.id()).orElseThrow();
        assertEquals(EmployeeStatus.DELETED, persisted.getStatus());
        assertNotNull(persisted.getDeletedAt());
        assertEquals(2, outboxEventRepository.count());
    }

    @Test
    @DisplayName("Deve gravar na outbox os eventos de ativação, desativação e exclusão")
    void statusChangesPersistPendingOutboxEventsWithTheirEmployeeStatuses() {
        // Preparação: cria colaborador, que também registra evento CREATED.
        EmployeeResponse created = createEmployee("Ana Silva", "ana@example.com");

        // Execução: executa as transições permitidas até excluir logicamente o colaborador.
        ResponseEntity<EmployeeResponse> activated = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/activate", null,
                EmployeeResponse.class);
        ResponseEntity<EmployeeResponse> deactivated = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT + "/" + created.id() + "/deactivate", null,
                EmployeeResponse.class);
        ResponseEntity<Void> deleted = restTemplate.exchange(
                EMPLOYEES_ENDPOINT + "/" + created.id(), HttpMethod.DELETE, null, Void.class);

        // Verificação: cada transição aceita gera exatamente um evento pendente com status correto.
        assertEquals(HttpStatus.OK, activated.getStatusCode());
        assertEquals(HttpStatus.OK, deactivated.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        java.util.List<OutboxEventJpaEntity> events =
                outboxEventRepository.findAll();
        assertEquals(4, events.size());
        assertOutboxEvent(events, EmployeeStatus.CREATED, "EmployeeCreatedEvent");
        assertOutboxEvent(events, EmployeeStatus.ACTIVE, "EmployeeStatusChangedEvent");
        assertOutboxEvent(events, EmployeeStatus.INACTIVE, "EmployeeStatusChangedEvent");
        assertOutboxEvent(events, EmployeeStatus.DELETED, "EmployeeStatusChangedEvent");
    }

    private void assertOutboxEvent(
            java.util.List<OutboxEventJpaEntity> events,
            EmployeeStatus status,
            String eventType) {
        OutboxEventJpaEntity event = events.stream()
                .filter(candidate -> candidate.getEmployeeStatus() == status)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Evento de outbox não encontrado para status " + status));
        assertEquals(eventType, event.getEventType());
        assertEquals("PENDING", event.getProcessingStatus().name());
        assertNotNull(event.getEventId());
        assertEquals(0, event.getRetryCount());
        assertNull(event.getPublishedAt());
        assertEquals(event.getCreatedAt(), event.getUpdateTime());
    }

    private EmployeeResponse createEmployee(String name, String email) {
        ResponseEntity<EmployeeResponse> response = restTemplate.postForEntity(
                EMPLOYEES_ENDPOINT, request(name, email), EmployeeResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private static CreateEmployeeRequest request(String name, String email) {
        return new CreateEmployeeRequest(name, email, DEPARTMENT_ID, POSITION_ID);
    }

    private static HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
