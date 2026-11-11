## 🚀 Quick Start

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

---

### 1. Subindo o Docker com a aplicação

Suba todos serviços necessários (Spring Boot App, PostgreSQL, Kafka e SonarQube):

```bash
# Devem ser executados na pasta raíz do projeto
cd ~/employees-manager

# Subir os containers
docker compose up --build -d

# Visualizar os logs da aplicação
docker compose logs -f app

# Parar todos containers
docker compose down
```

### 2. Testes Unitários

O serviço `app-test` executa os testes unitários e de integração. Os testes de integração usam Testcontainers para iniciar um PostgreSQL isolado; por isso, mantenha o Docker Desktop (ou Docker Engine) iniciado. O serviço monta o socket Docker do host e configura o endereço usado pelo Testcontainers para acessar as portas publicadas.

Ao subir os containers, o teste é automaticamente executado, caso queira executá-los novamente:

```bash
# Sobe o container de testes atualizado
docker compose build --no-cache app-test

# Executar testes unitários e de integração
docker compose run --rm app-test mvn clean verify
```

Os relatórios do JaCoCo e do Surefire podem ser vistos na pasta `app/reports`. O Testcontainers precisa de acesso ao socket Docker; execute apenas testes e código confiáveis pelo serviço `app-test`.

## 3. Kafka

O serviço `kafka-init` cria o tópico `hr.employee-events.v1` depois que o broker estiver saudável e antes de iniciar `app` ou `app-test`. O broker aceita conexões internas em `kafka:9092` e conexões da máquina host em `localhost:9092`.

O Outbox Poller publica eventos pendentes nesse tópico a cada 5 segundos. O producer aguarda o ACK do Kafka antes de marcar o evento como `PUBLISHED`; falhas incrementam `retry_count` e, ao atingir 5 tentativas, o evento é marcado como `FAILED`. Esses valores podem ser configurados por `OUTBOX_POLLER_INTERVAL`, `OUTBOX_POLLER_MAX_RETRIES` e `KAFKA_EMPLOYEE_EVENTS_TOPIC`.
