# Coding Guidelines

Estas são diretrizes que os agentes devem seguir ao gerar código para a aplicação:

## Nomenclatura
- O código deve ser sempre em inglês e utilizando termos em inglês para nomes de parâmetros, funções, classes e entidades;
- A documentação gerada seja no Swagger, logs ou comentários deve sem em Português do Brasil;

## HTTP
- As respostas de cada request HTTP ao sistema devem ser devidamente mapeadas de acordo com a solicitação realizada. Para casos de sucesso: 201 para recurso criado, 200 para request concluído, 202 para solicitação assíncrona concluída, etc.; Para casos de erro: 400, 404, 409, etc.

## Arquitetura obrigatória

- Aplicar **DDD real** com entidades ricas, agregados, value objects e services de domínio 
- Utilizar **Hexagonal Architecture** 
- Separar claramente as camadas: `domain`, `application`, `infrastructure`, `interfaces` 
- O domínio **não deve depender de frameworks**

## Persistência

- A aplicação deve utilizar **JPA**
- Pode ser usada uma das seguintes opções de banco de dados:
 - **PostgreSQL** (preferencial para produção ou uso real)
 - **H2 (em memória)** para testes e execução local simplificada
- Migrations obrigatórias com **Flyway**
- Scripts versionados na pasta `resources/db/migration`

## Testes

- **Testes unitários obrigatórios** com alta cobertura na camada de domínio e serviços de aplicação 
- Testes de integração com banco real (recomenda-se Testcontainers) 
- Testes de eventos (publicação e consumo) 
- Cobertura mínima sugerida: **80%**

## Versionamento de API
- Todos os endpoints REST devem ser versionados via URI usando o prefixo `/api/v1/` (ex: `/api/v1/employees`).

## Observabilidade & Logs
- **Spring Boot Actuator:** Expor endpoints `/actuator/health` e `/actuator/metrics`.
- **Logs Estruturados:** Utilizar Slf4j/Logback com saída em formato JSON (Logstash encoder) contendo `traceId`, `spanId` e `timestamp`.
- **Logs de Negócio:** Casos de uso e consumidores de eventos devem registrar logs informativos estruturados a cada transação relevante.

## Documentação (OpenAPI / Swagger)
- Todos os Controllers REST devem conter anotações do Springdoc (`@Operation`, `@ApiResponse`).
- O Swagger UI deve estar disponível publicamente no caminho `/swagger-ui.html`.
- O link de acesso direto deve estar destacado no `README.md`.