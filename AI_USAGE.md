# Declaração de Uso de IA

Acredito que a IA deva ser utilizada com o Human-in-the-Loop, cada iteração será avaliada por mim e trabalharei com a IA para obter a solução mais robusta, segura e otimizada. 

## Modo de Uso
Meu modo de uso dos agentes é prover um requisito de cada vez para evitar perda de contexto e reduzir as alucinações, além de facilitar a análise de cada trecho e poder iterar mais vezes em busca da melhor implementação.

## Ferramentas Utilizadas
- **Gemini / ChatGPT**: Refinamento de requisitos, modelagem arquitetural (DDD/Hexagonal) e revisão de regras de validação.
- **GitHub Copilot**: Autocompletar e implementação dos requisitos.

## Interações e Decisões Técnicas

### 1. Definição dos requisitos

- **Agente**: Gemini
- **Prompt**: Considerando a descrição do serviço de gerenciamento de funcionários a ser implementado, crie inicialmente apenas os requisitos funcionais e técnicos. Divida em histórias para desenvolvimento em ordem de execução.
- **Resultado/Decisão:**
    - Incluí manualmente a existência de endpoints para ativar e desativar o funcionário, que não estavam explícitos na descrição, nem foi mapeado pelo agente.
    - Tive que explicitar o uso de Outbox Pattern, o agente não tinha considerado em seu refinamento inicial.
    - Alterei a ordem de execução de algumas histórias, como configurar o Dockerfile e a pipeline, o que julgo ser importante ser feito imediatamente.
    - Explicitei como os campos status do Employee devem ser alterados nos endpoints e que o endpoint delete seria um soft delete para manter o histórico.

### 2. Melhor implementação do Outbox Pattern

- **Agente**: Gemini
- **Prompt**: Forneci o requisito de utilizar Outbox Pattern e a minha arquitetura inicial dele.
- **Resultado/Decisão:**
    - Utilizar apenas um tópico Kafka para os dois eventos ao invés de dois, a definição do tipo de evento será feito no Header da mensagem.
    - Usar o id do Employee como AggregateId no momento de publicar no Kafka, garantindo que os eventos cheguem na mesma partição e na mesma ordem.
    - Permitir que múltiplas instâncias do OutboxPoller busquem no banco de dados e publique em paralelo sem duplicidade.

### 3. Configuração do ambiente

- **Agente**: ChatGpt
- **Prompt**: Dados os requisitos técnicos da solução (Java, Spring Boot, SQL, Kafka, etc.) configure o scaffold do projeto, a configuração do docker-compose e Dockerfiles, e a pipeline do Github Actions que realizará build, test e sonar da aplicação Java.
- **Resultado/Decisão:** Foi definido a estrutura hexagonal como a esperada pela solução e configuração do ambiente com o passo-a-passo.

### 4. Implementação do RF01 - Camada de Negócio

- **Agente**: Copilot
- **Prompt**: Vamos implementar o RF01 que permite a criação de departamentos. Crie apenas os arquivos da application e domain layer, sem mexer na infraestrutura. Crie os testes unitários para testar apropriadamente o requisito e suas regras.
- **Resultado/Decisão:** Foram criados os domínios e validações para a entidade Departamento foram criadas; a camada de serviço que checa a existência de um departamento e envia para salvar e os testes unitários validando os casos de uso e possíveis exceções.

### 5. Implementação do RF01 - Banco de Dados

- **Agente**: Copilot
- **Prompt**: Para o RF01, crie a camada de infrastructure focando no banco de dados. Crie o arquivo Flyway para criação da tabela de departamento, o mapeamento da entidade do DB em código e a integração com o use case previamente criado via repository. Crie também os testes unitários que validam as novas camadas.
- **Resultado/Decisão:** Os campos de texto no SQL utilizarão TEXT já que no PostgreSQL o tipo TEXT e o VARCHAR utilizam exatamente o mesmo mecanismo de alocação de memória e usar TEXT evita alterar migrações do banco (via Flyway) no futuro caso o tamanho de um campo de nome de departamento precise expandir de 50 para 100 caracteres.

### 6. Implementação do RF01 - Camada API
- **Agente**: Copilot
- **Prompt**: Para o RF01, crie a camada de infrastructure focando na entrada via HTTP REST. Crie o endpoint de criação de departamento integre com a camada de negócios. Crie testes unitários garantindo o funcionamento desta camada.
- **Resultado/Decisão:** O endpoint de criação de departamento foi criado com seus devidos testes unitários. Foi necessário ajustes na formatação do timestamp com a IA e na localização adequada de alguns métodos.

### 7. Implementação do RF01 - Teste Integrado
- **Agente**: Copilot
- **Prompt**: Considere o RF01, crie fluxos de testes integrados que enviarão requests ao serviço e validarão a resposta e a persistência no banco de dados. Valide cenários de sucesso e de erros.
- **Resultado/Decisão:** O teste integrado foi criado validando o uso do endpoint de criação de departamentos de ponta a ponta.

### 8. Melhoria de Pipeline - Cobertura de Testes
- **Agente**: Copilot
- **Prompt**: Altere a pipeline atual do Github Actions para incluir como comentário a cobertura de testes do PR.
- **Resultado/Decisão:** Ao abrir PR, os jobs irão executar e a cobertura de testes será exibida, assim facilitando a avaliação pelo review.


### `A partir daqui, instruí à IA para automaticamente preencher esta documentação com as nossas iterações`


### 9. Implementação do RF02 - Camada de Negócio - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Vamos implementar o RF02 que permite a criação de cargos. Crie apenas os arquivos da application e domain layer, sem mexer na infraestrutura. Crie os testes unitários para testar apropriadamente o requisito e suas regras. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foram criados os value objects de título e nível, o agregado Cargo e seu caso de uso de criação com persistência via porta de saída. Os testes cobrem geração de identificador e data, normalização e campos obrigatórios; o nível permanece como texto, sem restringir os valores aos exemplos do requisito.

### 10. Implementação do RF02 - Banco de Dados - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Para o RF02, crie a camada de infrastructure focando no banco de dados. Crie o arquivo Flyway para criação da tabela de position, o mapeamento da entidade do DB em código e a integração com o use case previamente criado via repository. Crie também os testes unitários que validam as novas camadas. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foi criada a migration V2 para a tabela `position`, junto à entidade JPA, mapper e adapter que implementa `PositionRepositoryPort`. Os testes unitários cobrem o mapeamento dos campos e o fluxo de persistência, incluindo a propagação de falhas.

### 11. Implementação do RF02 - Camada API - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Para o RF02, crie a camada de infrastructure focando na entrada via HTTP REST. Crie o endpoint de criação de cargo integre com a camada de negócios. Crie testes unitários garantindo o funcionamento desta camada. Atualize o AI_USAGE.md a com nossa iteração.
- **Resultado/Decisão:** Foi criado o endpoint `POST /api/v1/positions`, com validação de título e nível, resposta `201 Created`, cabeçalho `Location` e timestamp formatado no fuso do projeto. O caso de uso foi registrado como bean para integração via injeção de dependência. Os testes unitários cobrem sucesso e rejeição de campos obrigatórios vazios.

### 12. Implementação do RF02 - Teste Integrado - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Considere o RF02, crie fluxos de testes integrados que enviarão requests ao /positions e validarão a resposta e a persistência no banco de dados. Valide cenários de sucesso e de erros. Atualize o item 12 do AI_USAGE em seguida.
- **Resultado/Decisão:** Foi criado teste de integração com requests HTTP reais para `/api/v1/positions` usando PostgreSQL via Testcontainers. Os cenários validam criação, resposta `201`, `Location`, fuso do timestamp e persistência de todos os campos, além de rejeição com `400` para título e nível em branco sem persistir registros.

### 13. Regra de unicidade do RF02 - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Vamos alterar o RF02. Caso já exista um cargo de mesmo title E level, não é necessário criar um novo. Altere os arquivos e testes para refletir esta alteração e responda devidamente ao usuário.
- **Resultado/Decisão:** A criação verifica a combinação normalizada de título e nível e responde HTTP 409 quando ela já existe. Foi adicionada constraint única no banco para proteger contra duplicações concorrentes; a violação dessa combinação é traduzida para a mesma exceção de domínio. Testes cobrem o caso duplicado, as demais combinações válidas e a resposta HTTP.

### 14. Normalização de título e nível do RF02 - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Após receber o CreatePositionCommand transforme os parâmetros title e level para titleCase. Assim ficará padronizado este formato ao salvar no banco. O objetivo é evitar que exista uma entrada: engenheiro  de software + junior e outra Engenheiro de Software + Junior no banco.
- **Resultado/Decisão:** Os value objects normalizam título e nível para title case e reduzem espaços consecutivos antes de consultar duplicidade ou persistir. O título mantém a preposição `de` em minúscula, conforme o exemplo solicitado. Os testes de domínio, caso de uso e integração foram atualizados para verificar a forma armazenada e rejeitar entradas equivalentes após normalização.

### 15. Implementação do RF03 - Camadas de Domínio e Aplicação - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Vamos implementar o RF03 que consiste em 6 endpoints. Crie apenas os arquivos da application e domain layer, sem mexer na infraestrutura. Crie os testes unitários para testar apropriadamente o requisito e suas regras.
- **Resultado/Decisão:** Foram implementados o agregado Employee, value objects, estados e regras de transição, além dos seis casos de uso: cadastro, listagem paginada, exclusão lógica, busca por departamento, ativação e desativação. O cadastro recebe departmentId e positionId como UUIDs, valida a existência das referências e a unicidade do e-mail, normalizado sem diferenciar caixa. As consultas paginadas usam offset/limit e excluem colaboradores DELETED. Ativar leva CREATED/INACTIVE a ACTIVE; desativar leva CREATED/ACTIVE a INACTIVE; transições inválidas são rejeitadas. Eventos do RF-04 e infraestrutura ficaram fora do escopo. Foram criados testes unitários de domínio e aplicação.

### 16. Implementação do RF03 - Banco de Dados - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Para o RF03, crie a camada de infrastructure focando no banco de dados. Crie o arquivo Flyway para criação da tabela de employee, o mapeamento da entidade do DB em código e a integração com o use case previamente criado via repository. Crie também os testes unitários que validam as novas camadas. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foi criada a migration V4 da tabela `employee`, com unicidade de e-mail, chaves estrangeiras para departamento e cargo, status e timestamps de auditoria. Foram adicionadas entidade JPA, mapper, repositório, adapters para persistência e lookup das referências, paginação literal por offset/limit e configuração dos seis casos de uso. Testes unitários cobrem mapeamento, persistência, lookup e exclusão de registros DELETED nas consultas.

### 17. Implementação do RF03 - Camada API - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Para o RF03, crie a camada de infrastructure focando na entrada via HTTP REST. Crie todos endpoints, integre com a camada de negócios. Crie testes unitários garantindo o funcionamento desta camada. Atualize o AI_USAGE.md a com nossa iteração.
- **Resultado/Decisão:** Foram criados os seis endpoints versionados em `/api/v1/employees`: cadastro, listagem paginada ou não, exclusão lógica, busca paginada por nome do departamento, ativação e desativação. Os controllers convertem requests em comandos e agregados em DTOs; validação e mapeamento de erros seguem as regras da aplicação, com respostas HTTP para sucesso, dados inválidos, recursos ausentes e e-mail duplicado. Testes unitários MVC cobrem os fluxos de entrada, validação, paginação e erros.

### 18. Testes Integrados do RF03 - API e Banco de Dados - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Considere o RF03, crie fluxos de testes integrados que enviarão requests ao /employees e validarão a resposta e a persistência no banco de dados para todos endpoints. Valide cenários de sucesso e de erros. Atualize o AI_USAGE em seguida.
- **Resultado/Decisão:** Foi criada uma suíte de integração com requests HTTP reais para os seis endpoints de `/api/v1/employees`, usando PostgreSQL via Testcontainers. Os cenários validam criação, listagem paginada e não paginada, busca paginada por departamento, exclusão lógica, ativação e desativação, verificando respostas e estado persistido. Também são cobertos e-mail duplicado, payload inválido, referências/departamento inexistentes, paginação inválida e transições de status proibidas.

### 19. Implementação do RF04 - Persistência de eventos na Outbox - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Considere o RF04, faça todas alterações necessárias para atendê-la (em todas camadas: negócio, infra, domain, etc.), crie o arquivo flyway para criação da tabela outbox_events. Crie também os testes unitários que validam todas alterações. Crie testes unitários que validam a publicação na tabela outbox_events para cada tipo de evento publicado.
- **Resultado/Decisão:** Foi adicionada a migration V5 e o mapeamento JPA da tabela `outbox_events`. Os casos de uso mantêm somente a operação `save(Employee)`; o adapter de persistência identifica criação ou mudança de status e grava o respectivo evento na mesma transação do colaborador. A outbox e seu status de processamento permanecem detalhes de infraestrutura. Por esclarecimento, os status de ativação/desativação gravados são `ACTIVE` e `INACTIVE`, alinhados ao domínio. Testes unitários e integrados cobrem os eventos de criação, ativação, desativação e exclusão lógica.

### 20. Preparação Kafka e implementação do RF05 - Outbox Poller - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Antes de implementar o RF05 para criar o Outbox Poller, crie a infraestrutura Kafka necessária no ambiente Docker de teste. Em seguida, implemente o poller na infraestrutura para buscar periodicamente eventos `PENDING`, publicar no tópico criado e atualizar `retry_count` e status em caso de sucesso ou falha.
- **Resultado/Decisão:** O Compose provisiona o tópico `hr.employee-events.v1` antes de iniciar a aplicação e os testes. O Outbox Poller executa a cada 5 segundos; a consulta usa `FOR UPDATE SKIP LOCKED` para permitir múltiplas instâncias sem selecionar os mesmos eventos. O producer idempotente envia o envelope do RF-05 com `eventId`, `eventType`, `aggregateId`, `occurredAt` e payload contendo `employeeId` e status, além do header `KafkaEmployeeEvent`. O poller aguarda o ACK antes de marcar como `PUBLISHED`; em falhas, incrementa `retry_count` e marca `FAILED` ao atingir 5 tentativas. O intervalo, o limite de retries e o tópico são configuráveis. Foram criados testes unitários para o poller e o publisher, mas nenhum teste integrado foi adicionado.

### 21. Implementação do RF06 - Consumo e histórico de eventos - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Implemente o RF06 com consumer Kafka para o tópico já criado, persistência na tabela `employee_events` via Flyway e todos os requisitos documentados. Crie testes unitários, sem testes integrados.
- **Resultado/Decisão:** Foi adicionada a migration V6, o modelo de domínio `EmployeeEvent`, o caso de uso para processar eventos e os adapters JPA e Kafka. `EmployeeCreatedEvent` é persistido como `CREATION` com status `CREATED`; `EmployeeStatusChangedEvent` é persistido como `UPDATE` com status `ACTIVE`, `INACTIVE` ou `DELETED`. O `eventId` permite detectar reentregas e evitar duplicidade. O consumer usa confirmação manual e propaga falhas de processamento para que o evento não seja confirmado. O adapter de persistência grava com `saveAndFlush` em uma transação `REQUIRES_NEW`, fazendo com que falhas de escrita ou commit sejam propagadas antes do retorno ao consumer. Foram adicionados testes unitários de caso de uso, consumer e adapter de persistência.

### 22. Testes Integrados do Fluxo RF05 + RF06 - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Crie testes integrados que insiram eventos em `outbox_events`, validem sua publicação pelo Outbox Poller no Kafka e o consumo e persistência em `employee_events`. Cubra também falhas, retries até o status `FAILED` e reentrega de eventos duplicados.
- **Resultado/Decisão:** Foi criada uma suíte de integração com PostgreSQL e Kafka via Testcontainers para cobrir o fluxo ponta a ponta, incluindo publicação/consumo, mapeamento de eventos de criação e alteração de status, persistência no histórico, duplicidade e esgotamento das tentativas na outbox.

### 23. Aprimoramento da observabilidade - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Aprimore a observabilidade de todo o serviço usando Spring Actuator para disponibilizar métricas, medir a duração dos métodos e facilitar a identificação de gargalos; adicione logs mais descritivos nos casos de uso e avalie ferramentas complementares.
- **Resultado/Decisão:** Foi adicionada a registry Prometheus e habilitada a exposição de métricas em `/actuator/prometheus`. Um aspecto instrumenta a duração e o resultado dos métodos dos casos de uso, do Outbox Poller e do consumer Kafka, registrando `hr.operation.duration` com tags de componente, método e resultado, histogramas e limites de latência. Os logs passaram a ser JSON estruturado em todos os perfis e incluem contexto da operação, duração, tipo de exceção e correlação por `traceId`/`spanId`; foram incluídos spans via Micrometer Tracing/Brave e observações do producer e listener Kafka.

### 24. Script para popular dados de teste local via API - *`AI ASSISTED`*

- **Agente**: Copilot
- **Prompt**: Crie um script para popular uma base de dados de teste local chamando os endpoints, com 3 departamentos, 6 cargos e 40 colaboradores; prefira uma alternativa que permita configurar quantidades e criar entidades aleatoriamente.
- **Resultado/Decisão:** Foi criado `scripts/seed-local.sh`, que usa Bash, `curl` e `jq` para criar departamentos, cargos e colaboradores pelos endpoints REST, respeitando a ordem e os vínculos por IDs retornados pela API. As quantidades e a URL base são configuráveis por variáveis de ambiente; os valores recebem identificadores por execução para evitar colisões ao repetir a carga, e erros HTTP interrompem o processo com a resposta recebida.
