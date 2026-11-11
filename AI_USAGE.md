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