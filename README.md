# HR Management System

Este é um desafio técnico para uma vaga de Engenheiro(a) de Software Sênior. 


## Visão geral

- **Banco padrão:** PostgreSQL. A configuração principal (`application.yml`) usa PostgreSQL em `localhost:5432` por padrão.
- **Perfil de teste:** H2 em memória para testes que usam `application-test.yml`. Os testes de integração substituem essa configuração por PostgreSQL e Kafka em Testcontainers.
- **Mensageria:** Apache Kafka, com o tópico `hr.employee-events.v1`.
- **Documentação da API:** [Swagger UI](http://localhost:8080/swagger-ui.html) e [OpenAPI JSON](http://localhost:8080/api-docs).
- **Atuadores:** `/actuator/health`, `/actuator/metrics` e `/actuator/prometheus`.

Para iniciar a aplicação e suas dependências por Docker Compose, siga o [Quick Start](QUICK_START.md). O processo de desenvolvimento com apoio de IA está registrado em [AI_USAGE.md](AI_USAGE.md).

## Endpoints

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Docs: http://localhost:8080/api-docs
- Spring Actuator Health: http://localhost:8080/actuator/health
- Spring Actuator Metrics: http://localhost:8080/actuator/metrics
- Spring Actuator Prometheus: http://localhost:8080/actuator/prometheus
- SonarQube Dashboard: http://localhost:9000 (Default login: admin / admin)

As métricas de duração das operações da aplicação ficam disponíveis em
`/actuator/metrics/hr.operation.duration`, com filtros por `component`, `method` e `outcome`.
O endpoint Prometheus expõe histogramas para análise de latência e percentis.

## 1. Definindo passo a passo

1. Definir os requisitos técnicos e funcionais do sistema;
2. Dados os requisitos, criar a arquitetura high level da solução;
3. Definir os contratos (payloads ou schemas de banco de dados);
4. Realizar um Review da Arquitetura e com esta análise realizar as alterações que julgar necessário;

``` A definição da arquitetura e requisitos técnicos é fundamental antes de começar a configuração e implementação dos serviços.```

5. Criar a configuração do Dockerfile com os requisitos técnicos;
6. Criar o *scaffold* da estrutura do projeto + testes;
7. Criar as entidades de domínio e testes simples para apenas validar o projeto;
8. Configurar o Github Actions para criar uma pipeline que realizará a build, Sonar e rodar testes para cada PR;
9. Realizar um commit inicial de teste;

``` Acredito que este commit inicial sem muita lógica de negócios, integrações com API/Kafkas/SQL facilita a identificação de erros de integração quando cada peça for acoplada nos futuros commits.```

10. Configurar boas práticas a serem seguidas pelo copilot;
11. A partir daqui, utilizar IA para auxiliar na implementação de cada requisito funcional do sistema. Começando pela camada de negócios e depois a integração com API, DB e Kafka. Deve ser feito um requisito de cada vez.

``` Implementar um requisito funcional de cada vez permite a melhor análise e teste de cada parte do sistema e evita perda de escopo.```

### Uso de IA
Todo uso de IA será documentado na página [AI Usage](AI_USAGE.md).

## 2. Requisitos Funcionais e Técnicos 

Após a leitura do desafio e análise dos requisitos do desafio, com o auxílio da IA, mapeei os requisitos técnicos e funcionais, assim como os domínios. 

Os requisitos podem ser vistos [aqui](docs/requisitos.md).

## 2.3 Observações Sobre Requisitos

### Employee Status
Ao analisar a solução percebi que o Employee ter um campo "status" e o trecho:
```Ao inativar um colaborador, dispare EmployeeStatusChangedEvent``` indicam a necessidade de um endpoint para alterar o status do Employee após sua criação. <br>

Extrapolei que o status poderia ser: "CREATED", "INACTIVE" e "ACTIVE" e que o funcionário seria criado como PENDING inicialmente. <br>

Ativar e desativar um funcionário pode desencadear em muitas ações além da alteração no banco, seguindo o DDD faria mais sentido, então, dois endpoints: /activate e /deactivate.

## 3. Solution Architecture

Após o entendimento da solução como um todo e a definição dos requisitos, criei com auxílio de IA o diagrama abaixo da arquitetura inicialmente definida:

```mermaid
graph LR
    User([User]) 

    subgraph Service ["Spring Boot Service"]
        direction TB
        Controller[REST Controllers]
        Domain[Domain Services & Use Cases]
        
        subgraph OutboxModule ["Transactional Outbox Component"]
            OutboxRepo[Outbox Repository]
            Poller["Outbox Poller<br/>(@Scheduled Worker)"]
        end

        Producer[Kafka Event Publisher Adapter]
        Consumer[Kafka Event Consumer Adapter]
        Persistence[JPA Persistence Adapter]
    end

    subgraph DB ["Database (PostgreSQL)"]
        T_Emp[(employees / departments / positions)]
        T_Outbox[(outbox_events)]
        T_Audit[(employee_events)]
    end

    Kafka{{Kafka Broker<br/>Topic: hr.employee-events.v1}}

    %% Synchronous HTTP flow
    User -->|1. HTTP Request| Controller
    Controller -->|HTTP Response| User
    
    %% Internal layers & Atomic Transaction
    Controller --> Domain
    Domain -->|Save Entity| Persistence
    Domain -->|Save Event Payload| OutboxRepo
    Persistence -->|2a. Write Domain Entity| T_Emp
    OutboxRepo -->|2b. Write Event PENDING| T_Outbox
    
    %% Outbox Poller Flow
    Poller -->|3. Read PENDING Events| T_Outbox
    Poller -->|4. Hand off Event| Producer
    Producer -->|5. Publish Event| Kafka
    Poller -->|6. Mark as PUBLISHED| T_Outbox

    %% Async Consumer Flow
    Kafka -->|7. Consume Event| Consumer
    Consumer -->|8. Write Audit Log| T_Audit
```

## 4. Configurando o ambiente

Utilizei a IA para definir qual versão de Java e Spring Boot são mais adequadas para uma melhor compatibilidade com todas integrações, obtendo-se, então:  **Java 21 (LTS) e Spring Boot 3.3.5**.

Com estas definições foi permitido gerar o scaffold da aplicação, a configuração do Docker e a pipeline do Github Actions para build e testes em PR.

## 5. Observações Durante Desenvolvimento

### Endpoint /positions

Ao implementar este endpoint percebi que do jeito que fiz o refinamento poderia haver muitas entradas repetidas da mesma combinação de Título+Level de cargo, então adicionei como requisito que deve-se validar a existência desta combinação na tabela antes de criar o cargo.

## 6. Organização do código

- `domain`: modelo de negócio, value objects, exceções e regras de transição.
- `application`: casos de uso e portas inbound/outbound.
- `infrastructure/adapters/inbound/rest`: endpoints, DTOs e tratamento de erros HTTP.
- `infrastructure/adapters/inbound/messaging`: consumer Kafka.
- `infrastructure/adapters/outbound/persistence`: entidades JPA, repositórios, mappers e adapters.
- `infrastructure/adapters/outbound/messaging`: producer Kafka e Outbox Poller.
- `infrastructure/config` e `infrastructure/observability`: configuração de infraestrutura, métricas e tracing.

As alterações do colaborador e a entrada correspondente na outbox são persistidas na mesma transação. A publicação Kafka ocorre posteriormente, fora da requisição HTTP.

## 7. API REST Construído

Todos os recursos usam o prefixo `/api/v1`. A tabela resume os endpoints disponíveis; a documentação interativa apresenta schemas e respostas detalhadas.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/departments` | Criar departamento |
| `POST` | `/api/v1/positions` | Criar cargo |
| `POST` | `/api/v1/employees` | Criar colaborador |
| `GET` | `/api/v1/employees` | Listar colaboradores, opcionalmente paginados |
| `GET` | `/api/v1/employees/search?department={name}` | Buscar colaboradores por departamento |
| `POST` | `/api/v1/employees/{id}/activate` | Ativar colaborador |
| `POST` | `/api/v1/employees/{id}/deactivate` | Desativar colaborador |
| `DELETE` | `/api/v1/employees/{id}` | Excluir logicamente (status `DELETED`) |

Para paginação, informe `offset` e `limit` juntos. Sem eles, a listagem retorna a coleção não paginada.

### Exemplos de chamadas

Criar um departamento:

```bash
curl --request POST http://localhost:8080/api/v1/departments \
  --header "Content-Type: application/json" \
  --data '{"name":"Engenharia","costCenterCode":"CC-100"}'
```

Criar um cargo:

```bash
curl --request POST http://localhost:8080/api/v1/positions \
  --header "Content-Type: application/json" \
  --data '{"title":"Engenheiro de Software","level":"Senior"}'
```

Criar um colaborador. Use os UUIDs retornados nas respostas dos dois requests anteriores:

```bash
curl --request POST http://localhost:8080/api/v1/employees \
  --header "Content-Type: application/json" \
  --data '{
    "name":"Ana Silva",
    "email":"ana.silva@example.com",
    "departmentId":"<department-uuid>",
    "positionId":"<position-uuid>"
  }'
```

Listar a primeira página e buscar por departamento:

```bash
curl "http://localhost:8080/api/v1/employees?offset=0&limit=20"
curl "http://localhost:8080/api/v1/employees/search?department=Engenharia&offset=0&limit=20"
```

## 8. Eventos assíncronos

1. A criação ou mudança de status do colaborador grava uma entrada `PENDING` em `outbox_events` na mesma transação da alteração do colaborador.
2. O Outbox Poller consulta periodicamente eventos pendentes e publica no tópico `hr.employee-events.v1`. O uso de locks no banco permite que múltiplos pollers processem eventos concorrentes sem selecionar a mesma entrada ao mesmo tempo.
3. O producer aguarda o ACK do Kafka. Após sucesso, a outbox passa a `PUBLISHED`; falhas incrementam `retry_count` e, ao alcançar o limite configurado, a entrada passa a `FAILED`.
4. O consumer processa `EmployeeCreatedEvent` e `EmployeeStatusChangedEvent`, registra o histórico em `employee_events` e só confirma a mensagem depois que a persistência conclui com sucesso. O `eventId` permite reconhecer duplicatas.

O evento usa a chave do colaborador como chave Kafka, o header `KafkaEmployeeEvent` e um envelope contendo `eventId`, `eventType`, `aggregateId`, `occurredAt` e payload. Consulte a documentação Swagger e a configuração da aplicação para os parâmetros de execução.

