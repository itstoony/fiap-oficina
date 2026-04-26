# Oficina Mecânica — API REST

**FIAP Pós-Tech Software Architecture — Tech Challenge Fase 1**

Backend MVP de um sistema integrado de atendimento e execução de serviços de uma oficina mecânica de médio porte. Clientes acompanham em tempo real o andamento do serviço e autorizam reparos adicionais via API.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [O que está implementado](#o-que-está-implementado)
- [O que será implementado](#o-que-será-implementado)
- [Como rodar com Docker Compose](#como-rodar-com-docker-compose)
- [Como rodar localmente (sem Docker)](#como-rodar-localmente-sem-docker)
- [Endpoints disponíveis](#endpoints-disponíveis)
- [Documentação interativa (Swagger)](#documentação-interativa-swagger)
- [Testes](#testes)

---

## Visão Geral

O sistema permite que uma oficina mecânica gerencie todo o ciclo de atendimento:

1. Cadastro de clientes (CPF/CNPJ) e veículos (placa)
2. Abertura e acompanhamento de Ordens de Serviço (OS)
3. Diagnóstico, orçamento e aprovação pelo cliente via API pública
4. Execução dos serviços com controle de estoque de peças (reserva e baixa)
5. Finalização e entrega do veículo
6. Relatórios administrativos e catálogo de serviços

A documentação DDD completa (Event Storming, Linguagem Ubíqua, Aggregates, Bounded Contexts) está disponível no [Miro](https://miro.com/app/board/uXjVGjwS_Y8=/).

---

## Arquitetura

Monolito em camadas com organização interna por **Bounded Context** (DDD). Cada contexto é um pacote isolado com suas próprias camadas.

```
br.com.fiap.oficina/
├── atendimento/       ← Clientes e Veículos
├── execucao/          ← Ordens de Serviço (a implementar)
├── estoque/           ← Peças e Estoque (a implementar)
├── administracao/     ← Catálogo e Relatórios (a implementar)
├── seguranca/         ← JWT e Autenticação (a implementar)
└── shared/            ← Exceções globais
```

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.4.5 |
| Spring Data JPA | — |
| Spring Security | — |
| jjwt | 0.12.6 |
| SpringDoc OpenAPI | 2.8.4 |
| PostgreSQL | 16 |
| H2 (testes) | — |
| Lombok | — |
| JaCoCo | 0.8.12 |
| Docker / Docker Compose | — |

---

## O que está implementado

### Infraestrutura Compartilhada

- `RecursoNaoEncontradoException` — HTTP 404
- `RegraDeNegocioException` — HTTP 422
- `GlobalExceptionHandler` — respostas JSON padronizadas para todos os erros
- `SecurityConfig` — configuração temporária (endpoints liberados até implementação do JWT)
- `OpenApiConfig` — Swagger UI com esquema `bearerAuth`

### Bounded Context: Atendimento ao Cliente

**Value Objects com validação de domínio:**

| Classe | Descrição |
|---|---|
| `Documento` | Encapsula CPF ou CNPJ. Valida pelo algoritmo completo dos dígitos verificadores. Aceita com ou sem formatação |
| `Placa` | Valida e normaliza placas no formato antigo (ABC1234) e Mercosul (ABC1D23) |
| `TipoDocumento` | Enum `CPF` / `CNPJ` |

**Entidades:**

| Classe | Descrição |
|---|---|
| `Cliente` | Aggregate Root. Pessoa física (CPF) ou jurídica (CNPJ) |
| `Veiculo` | Associado a um cliente, identificado pela placa |
| `Atendente` | Mecânico vinculado à OS |

**Endpoints disponíveis** (todos exigirão JWT após implementação da segurança):

```
POST   /api/admin/clientes
GET    /api/admin/clientes
GET    /api/admin/clientes/{id}
GET    /api/admin/clientes/documento/{documento}
PUT    /api/admin/clientes/{id}
DELETE /api/admin/clientes/{id}

POST   /api/admin/veiculos
GET    /api/admin/veiculos
GET    /api/admin/veiculos/{id}
GET    /api/admin/veiculos/placa/{placa}
GET    /api/admin/veiculos/cliente/{clienteId}
PUT    /api/admin/veiculos/{id}
DELETE /api/admin/veiculos/{id}
```

**Regras de negócio aplicadas:**
- CPF validado pelo algoritmo dos dois dígitos verificadores
- CNPJ validado pelo algoritmo padrão de 14 dígitos
- Documento duplicado retorna HTTP 422
- Placa duplicada retorna HTTP 422
- Placa normalizada para maiúsculas ao persistir

**Testes:**
- 12 testes unitários — `ClienteServiceTest`
- 9 testes unitários — `VeiculoServiceTest`
- 10 testes de integração MockMvc — `ClienteControllerTest`
- 11 testes de integração MockMvc — `VeiculoControllerTest`

---

## O que será implementado

### Bounded Context: Segurança (JWT)

Autenticação e autorização de todos os endpoints `/api/admin/**`.

- `POST /api/auth/login` — retorna token JWT
- Filtro JWT validando `Authorization: Bearer <token>`
- Endpoints públicos: `/api/public/**`, `/swagger-ui.html`, `/api-docs/**`

### Bounded Context: Execução de Serviços

Ciclo de vida completo da Ordem de Serviço com máquina de estados:

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                  ↘ CANCELADA
```

- Abertura de OS com vínculo a cliente e veículo
- Adição de serviços e peças com cálculo automático do orçamento
- Transições de status via endpoints admin
- Aprovação/recusa pelo cliente via endpoint **público** (sem JWT)
- Consulta pública de status da OS por número
- Integração com Estoque ao iniciar execução (converte reservas em baixas)

### Bounded Context: Estoque e Insumos

Gestão de peças com controle de disponibilidade em tempo real.

- `qtdDisponivel = qtdEstoque - qtdReservada` (calculado, nunca persistido)
- Reserva (soft-lock) ao vincular peça a uma OS
- Baixa definitiva ao aprovar a OS
- Liberação de reservas ao cancelar a OS
- Alerta de estoque crítico quando `qtdEstoque <= qtdMinima`
- Histórico de movimentações por peça

### Bounded Context: Gestão Administrativa

- CRUD do catálogo de serviços
- Relatório de tempo médio de execução de OSs finalizadas

---

## Como rodar com Docker Compose

### Pré-requisitos

- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/) instalados

### 1. Clone o repositório

```bash
git clone https://github.com/itstoony/fia-oficina.git
cd fia-oficina
```

### 2. Gere o JAR

```bash
./mvnw clean package -DskipTests
```

### 3. Suba os containers

```bash
docker compose up --build
```

Isso irá subir dois serviços:
- **db** — PostgreSQL 16 na porta `5432`
- **app** — API Spring Boot na porta `8080`

A aplicação aguarda o banco estar saudável antes de iniciar (`healthcheck` configurado).

### 4. Acesse a API

| Recurso | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

### 5. Parar os containers

```bash
docker compose down
```

Para remover também o volume do banco:

```bash
docker compose down -v
```

---

## Como rodar localmente (sem Docker)

### Pré-requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL rodando localmente

### 1. Crie o banco de dados

```sql
CREATE DATABASE oficina;
CREATE USER oficina WITH PASSWORD 'oficina';
GRANT ALL PRIVILEGES ON DATABASE oficina TO oficina;
```

### 2. Execute a aplicação

```bash
./mvnw spring-boot:run
```

---

## Endpoints disponíveis

### Clientes

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/clientes` | Cadastrar cliente (CPF ou CNPJ) |
| `GET` | `/api/admin/clientes` | Listar todos os clientes |
| `GET` | `/api/admin/clientes/{id}` | Buscar cliente por ID |
| `GET` | `/api/admin/clientes/documento/{doc}` | Buscar por CPF/CNPJ |
| `PUT` | `/api/admin/clientes/{id}` | Atualizar dados do cliente |
| `DELETE` | `/api/admin/clientes/{id}` | Excluir cliente |

**Exemplo — cadastrar cliente:**

```bash
curl -X POST http://localhost:8080/api/admin/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@email.com",
    "telefone": "11999999999",
    "documento": "529.982.247-25"
  }'
```

### Veículos

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/veiculos` | Cadastrar veículo |
| `GET` | `/api/admin/veiculos` | Listar todos os veículos |
| `GET` | `/api/admin/veiculos/{id}` | Buscar veículo por ID |
| `GET` | `/api/admin/veiculos/placa/{placa}` | Buscar por placa |
| `GET` | `/api/admin/veiculos/cliente/{clienteId}` | Listar veículos de um cliente |
| `PUT` | `/api/admin/veiculos/{id}` | Atualizar dados do veículo |
| `DELETE` | `/api/admin/veiculos/{id}` | Excluir veículo |

**Exemplo — cadastrar veículo:**

```bash
curl -X POST http://localhost:8080/api/admin/veiculos \
  -H "Content-Type: application/json" \
  -d '{
    "marca": "Toyota",
    "modelo": "Corolla",
    "ano": 2020,
    "cor": "Prata",
    "placa": "ABC-1234",
    "clienteId": "<uuid-do-cliente>"
  }'
```

### Formato de erros

Todos os erros retornam JSON padronizado:

```json
{
  "timestamp": "2026-04-25T15:00:00",
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Já existe um cliente cadastrado com este documento"
}
```

Erros de validação incluem o campo `campos` com detalhes por campo:

```json
{
  "timestamp": "2026-04-25T15:00:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Erro de validação",
  "campos": {
    "email": "must not be blank",
    "documento": "must not be blank"
  }
}
```

---

## Documentação interativa (Swagger)

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

---

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar com relatório de cobertura JaCoCo
./mvnw verify
```

O relatório de cobertura é gerado em `target/site/jacoco/index.html`.

Cobertura mínima configurada: **80%** nas classes de domínio (excluindo DTOs, configs e exceptions).
