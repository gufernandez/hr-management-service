## 🚀 Quick Start

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

---

### 1. Subindo o Docker com a aplicação

Suba todos serviços necessários (Spring Boot App, PostgreSQL, Kafka, SonarQube e MCP Server):

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

Criei um Dockerfile.dev específico para testes unitários de nossa aplicação. Após subir todos containers, basta rodar o comando:

```bash
docker compose run --rm app-test
```