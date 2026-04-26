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
├── atendimento/       ← Clientes e Veículos ✓
├── execucao/          ← Ordens de Serviço ✓
├── estoque/           ← Peças e Estoque (stub de integração) ✓
├── administracao/     ← Catálogo de Serviços (stub) ✓
├── seguranca/         ← JWT e Autenticação (a implementar)
└── shared/            ← Exceções globais ✓
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

### Bounded Context: Execução de Serviços

**Máquina de estados do StatusOS:**

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                  ↘
                                               CANCELADA
```

Transições são unidirecionais. Qualquer tentativa de transição inválida retorna HTTP 422.

**Aggregate e Entidades:**

| Classe | Descrição |
|---|---|
| `OrdemDeServico` | Aggregate Root. Controla todo o ciclo de vida da OS |
| `ItemServico` | Serviço do catálogo vinculado a uma OS (quantidade + preço capturado no momento) |
| `ItemPeca` | Peça vinculada a uma OS (quantidade + preço capturado no momento) |
| `StatusOS` | Enum com validação de transição via `validarTransicaoPara()` |

**Regras de negócio aplicadas:**
- Número gerado automaticamente no formato `OS-{ano}-{sequencial 5 dígitos}` (ex: `OS-2026-00001`)
- `valorTotal` = Σ(quantidade × precoUnitario) para serviços + peças, recalculado automaticamente
- `dataInicioExecucao` registrada ao transicionar para `EM_EXECUCAO`
- `dataFimExecucao` registrada ao transicionar para `FINALIZADA`
- Ao iniciar execução: peças reservadas são convertidas em baixas definitivas via `EstoqueService`
- Ao cancelar: todas as reservas de peças são liberadas via `EstoqueService`
- Edição de itens bloqueada quando OS está em `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE` ou `CANCELADA`
- Reserva de estoque ao adicionar peça: verifica disponibilidade (`qtdEstoque - qtdReservada >= qtdSolicitada`)

**Integração com Estoque (EstoqueService):**

| Operação | Quando |
|---|---|
| `verificarDisponibilidadeEReservar` | Ao adicionar peça à OS |
| `liberarReserva` | Ao remover peça da OS ou cancelar a OS |
| `baixarEstoque` | Ao iniciar execução (aprovação) — converte reserva em baixa definitiva |

**Endpoints admin** (exigirão JWT após implementação da segurança):

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/ordens` | Abrir nova OS |
| `GET` | `/api/admin/ordens` | Listar todas as OSs |
| `GET` | `/api/admin/ordens/{id}` | Buscar OS por ID |
| `POST` | `/api/admin/ordens/{id}/servicos` | Adicionar serviço à OS |
| `DELETE` | `/api/admin/ordens/{id}/servicos/{itemId}` | Remover serviço da OS |
| `POST` | `/api/admin/ordens/{id}/pecas` | Adicionar peça à OS (reserva estoque) |
| `DELETE` | `/api/admin/ordens/{id}/pecas/{itemId}` | Remover peça da OS (libera reserva) |
| `POST` | `/api/admin/ordens/{id}/iniciar-diagnostico` | Transição RECEBIDA → EM_DIAGNOSTICO |
| `POST` | `/api/admin/ordens/{id}/enviar-orcamento` | Transição EM_DIAGNOSTICO → AGUARDANDO_APROVACAO |
| `POST` | `/api/admin/ordens/{id}/iniciar-execucao` | Transição AGUARDANDO_APROVACAO → EM_EXECUCAO |
| `POST` | `/api/admin/ordens/{id}/finalizar` | Transição EM_EXECUCAO → FINALIZADA |
| `POST` | `/api/admin/ordens/{id}/entregar` | Transição FINALIZADA → ENTREGUE |
| `POST` | `/api/admin/ordens/{id}/cancelar` | Cancelar OS (antes de EM_EXECUCAO) |

**Endpoints públicos** (sem autenticação):

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/public/ordens/{numero}/status` | Consultar status da OS por número |
| `POST` | `/api/public/ordens/{numero}/aprovar` | Cliente aprova o orçamento |
| `POST` | `/api/public/ordens/{numero}/recusar` | Cliente recusa o orçamento |

**Testes:**
- 14 testes unitários — `StatusOSTest` (todas as transições válidas e inválidas)
- 14 testes unitários — `OrdemDeServicoServiceTest`
- 8 testes de integração MockMvc — `OrdemDeServicoAdminControllerTest`
- 5 testes de integração MockMvc — `OrdemDeServicoPublicControllerTest`

---

## O que será implementado

### Bounded Context: Segurança (JWT)

Autenticação e autorização de todos os endpoints `/api/admin/**`.

- `POST /api/auth/login` — retorna token JWT
- Filtro JWT validando `Authorization: Bearer <token>`
- Endpoints públicos: `/api/public/**`, `/swagger-ui.html`, `/api-docs/**`

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

### Ordens de Serviço

**Exemplo — abrir OS:**

```bash
curl -X POST http://localhost:8080/api/admin/ordens \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "<uuid-do-cliente>",
    "veiculoId": "<uuid-do-veiculo>",
    "observacoes": "Veículo com barulho no motor"
  }'
```

**Exemplo — consultar status (público):**

```bash
curl http://localhost:8080/api/public/ordens/OS-2026-00001/status
```

**Exemplo — cliente aprovar orçamento (público):**

```bash
curl -X POST http://localhost:8080/api/public/ordens/OS-2026-00001/aprovar
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

### Suíte atual de testes

| Contexto | Classe de Teste | Testes |
|---|---|---|
| Atendimento | `ClienteServiceTest` | 12 |
| Atendimento | `VeiculoServiceTest` | 9 |
| Atendimento | `ClienteControllerTest` | 10 |
| Atendimento | `VeiculoControllerTest` | 11 |
| Execução | `StatusOSTest` | 14 |
| Execução | `OrdemDeServicoServiceTest` | 14 |
| Execução | `OrdemDeServicoAdminControllerTest` | 8 |
| Execução | `OrdemDeServicoPublicControllerTest` | 5 |
| Integração | `OficinaApplicationTests` | 1 |
| **Total** | | **84** |
