# Declaração de Uso de IA

Acredito que a IA deva ser utilizada com o Human-in-the-Loop, cada iteração será avaliada por mim e trabalharei com a IA para obter a solução mais robusta, segura e otimizada. 

## Modo de Uso
Meu modo de uso dos agentes é prover um requisito de cada vez para evitar perda de contexto e reduzir as alucinações, além de facilitar a análise de cada trecho e poder iterar mais vezes em busca da melhor implementação.

## Ferramentas Utilizadas
- **Gemini / ChatGPT**: Refinamento de requisitos, modelagem arquitetural (DDD/Hexagonal) e revisão de regras de validação.
- **GitHub Copilot / Clause**: Autocompletar e implementação dos requisitos.

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

### 9. Implementação do RF02 - Camada de Negócio

- **Agente**: Copilot
- **Prompt**: Vamos implementar o RF02 que permite a criação de cargos. Crie apenas os arquivos da application e domain layer, sem mexer na infraestrutura. Crie os testes unitários para testar apropriadamente o requisito e suas regras. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foram criados os value objects de título e nível, o agregado Cargo e seu caso de uso de criação com persistência via porta de saída. Os testes cobrem geração de identificador e data, normalização e campos obrigatórios; o nível permanece como texto, sem restringir os valores aos exemplos do requisito.

### 10. Implementação do RF02 - Banco de Dados

- **Agente**: Copilot
- **Prompt**: Para o RF02, crie a camada de infrastructure focando no banco de dados. Crie o arquivo Flyway para criação da tabela de position, o mapeamento da entidade do DB em código e a integração com o use case previamente criado via repository. Crie também os testes unitários que validam as novas camadas. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foi criada a migration V2 para a tabela `position`, junto à entidade JPA, mapper e adapter que implementa `PositionRepositoryPort`. Os testes unitários cobrem o mapeamento dos campos e o fluxo de persistência, incluindo a propagação de falhas; nenhum teste foi executado a pedido do usuário.

### 11. Implementação do RF02 - Camada API

- **Agente**: Copilot
- **Prompt**: Para o RF02, crie a camada de infrastructure focando na entrada via HTTP REST. Crie o endpoint de criação de cargo integre com a camada de negócios. Crie testes unitários garantindo o funcionamento desta camada. Atualize o AI_USAGE.md a com nossa iteração.
- **Resultado/Decisão:** Foi criado o endpoint `POST /api/v1/positions`, com validação de título e nível, resposta `201 Created`, cabeçalho `Location` e timestamp formatado no fuso do projeto. O caso de uso foi registrado como bean para integração via injeção de dependência. Os testes unitários cobrem sucesso e rejeição de campos obrigatórios vazios; não foram executados.

### 12. Implementação do RF02 - Teste Integrado

- **Agente**: Copilot
- **Prompt**: Considere o RF02, crie fluxos de testes integrados que enviarão requests ao /positions e validarão a resposta e a persistência no banco de dados. Valide cenários de sucesso e de erros. Atualize o item 12 do AI_USAGE em seguida.
- **Resultado/Decisão:** Foi criado teste de integração com requests HTTP reais para `/api/v1/positions` usando PostgreSQL via Testcontainers. Os cenários validam criação, resposta `201`, `Location`, fuso do timestamp e persistência de todos os campos, além de rejeição com `400` para título e nível em branco sem persistir registros. Os testes não foram executados.

### 13. Regra de unicidade do RF02

- **Agente**: Copilot
- **Prompt**: Vamos alterar o RF02. Caso já exista um cargo de mesmo title E level, não é necessário criar um novo. Altere os arquivos e testes para refletir esta alteração e responda devidamente ao usuário.
- **Resultado/Decisão:** A criação verifica a combinação normalizada de título e nível e responde HTTP 409 quando ela já existe. Foi adicionada constraint única no banco para proteger contra duplicações concorrentes; a violação dessa combinação é traduzida para a mesma exceção de domínio. Testes cobrem o caso duplicado, as demais combinações válidas e a resposta HTTP. Os testes não foram executados.

### 14. Normalização de título e nível do RF02

- **Agente**: Copilot
- **Prompt**: Após receber o CreatePositionCommand transforme os parâmetros title e level para titleCase. Assim ficará padronizado este formato ao salvar no banco. O objetivo é evitar que exista uma entrada: engenheiro  de software + junior e outra Engenheiro de Software + Junior no banco.
- **Resultado/Decisão:** Os value objects normalizam título e nível para title case e reduzem espaços consecutivos antes de consultar duplicidade ou persistir. O título mantém a preposição `de` em minúscula, conforme o exemplo solicitado. Os testes de domínio, caso de uso e integração foram atualizados para verificar a forma armazenada e rejeitar entradas equivalentes após normalização. Os testes não foram executados.

### 15. Implementação do RF03 - Camadas de Domínio e Aplicação

- **Agente**: Copilot
- **Prompt**: Vamos implementar o RF03 que consiste em 6 endpoints. Crie apenas os arquivos da application e domain layer, sem mexer na infraestrutura. Crie os testes unitários para testar apropriadamente o requisito e suas regras.
- **Resultado/Decisão:** Foram implementados o agregado Employee, value objects, estados e regras de transição, além dos seis casos de uso: cadastro, listagem paginada, exclusão lógica, busca por departamento, ativação e desativação. O cadastro recebe departmentId e positionId como UUIDs, valida a existência das referências e a unicidade do e-mail, normalizado sem diferenciar caixa. As consultas paginadas usam offset/limit e excluem colaboradores DELETED. Ativar leva CREATED/INACTIVE a ACTIVE; desativar leva CREATED/ACTIVE a INACTIVE; transições inválidas são rejeitadas. Eventos do RF-04 e infraestrutura ficaram fora do escopo. Foram criados testes unitários de domínio e aplicação; não foram executados. A compilação também não foi verificada porque Maven e wrapper não estavam disponíveis no ambiente.

### 16. Implementação do RF03 - Banco de Dados

- **Agente**: Copilot
- **Prompt**: Para o RF03, crie a camada de infrastructure focando no banco de dados. Crie o arquivo Flyway para criação da tabela de employee, o mapeamento da entidade do DB em código e a integração com o use case previamente criado via repository. Crie também os testes unitários que validam as novas camadas. Atualize o arquivo AI_USAGE.md com a nossa iteração.
- **Resultado/Decisão:** Foi criada a migration V4 da tabela `employee`, com unicidade de e-mail, chaves estrangeiras para departamento e cargo, status e timestamps de auditoria. Foram adicionadas entidade JPA, mapper, repositório, adapters para persistência e lookup das referências, paginação literal por offset/limit e configuração dos seis casos de uso. Testes unitários cobrem mapeamento, persistência, lookup e exclusão de registros DELETED nas consultas. Os testes não foram executados.
