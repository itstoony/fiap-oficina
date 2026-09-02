# Oficina Mecânica — API REST

**FIAP Pós-Tech Software Architecture — Tech Challenge Fase 3**

Backend de um sistema integrado de atendimento e execução de serviços de uma oficina mecânica. Clientes acompanham em tempo real o andamento do serviço e autorizam reparos adicionais via API.

---

## Fase 3 — Serverless, AWS EKS e RDS

### Objetivos

- **Autenticação de clientes via Lambda** — função serverless em Python que valida CPF e retorna JWT (repositório `oficina-lambda`)
- **Deploy em AWS EKS** — cluster Kubernetes gerenciado na AWS (repositório `oficina-infra-k8s`)
- **Banco de dados RDS PostgreSQL** — instância gerenciada na AWS (repositório `oficina-infra-db`)
- **Pipeline CI/CD** com GitHub Actions: build → testes → push para ECR → `kubectl set image` no EKS
- **Autenticação admin** via `/api/auth/admin` (login/senha) para operações administrativas

### Arquitetura de Deploy

```
┌────────────────── GitHub Actions CI/CD ───────────────────────┐
│  Push main → Build → Test → Push ECR → kubectl set image EKS  │
└──────────────────────────────────────────────────────────┬────┘
                                                           │
┌──── AWS EKS (oficina-cluster, t3.micro) ─────────────────▼───┐
│                                                               │
│  ┌─────────────────┐        ┌──────────────────────────────┐ │
│  │  oficina-app    │◄──────►│  RDS PostgreSQL 16.9         │ │
│  │  (1 pod)        │        │  oficina-db (db.t3.micro)    │ │
│  │  HPA: CPU > 70% │        └──────────────────────────────┘ │
│  └────────┬────────┘                                          │
│  ┌────────▼────────┐                                          │
│  │  LoadBalancer   │                                          │
│  │  porta 80       │                                          │
│  └─────────────────┘                                          │
└───────────────────────────────────────────────────────────────┘
         │
         ▼
┌──── AWS Lambda (oficina-lambda) ──────────────┐
│  POST /auth/login → valida CPF → retorna JWT  │
│  API Gateway: fpwmtfk2k4.execute-api...       │
└───────────────────────────────────────────────┘
```

### Fluxo de autenticação

| Quem | Endpoint | Como |
|---|---|---|
| **Administrador** | `POST /api/auth/admin` | login + senha → JWT |
| **Cliente** | `POST <lambda>/auth/login` | CPF → JWT (via Lambda) |

### Secrets necessários no GitHub Actions

| Secret | Descrição |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS |

### Deploy e demonstração

> **Nota sobre custos:** A infraestrutura (EKS + RDS) não é mantida permanentemente no ar devido aos custos da AWS (cluster EKS ~$0.10/h, sem free tier). O ambiente é provisionado sob demanda via `terraform apply` no repositório `oficina-infra-k8s` e destruído após uso. Todo o funcionamento do sistema está demonstrado no vídeo abaixo.

| Recurso | URL |
|---|---|
| **Vídeo de demonstração** | *(a preencher após gravação)* |
| **Lambda (CPF auth)** | `https://fpwmtfk2k4.execute-api.sa-east-1.amazonaws.com/Prod/auth/login` |
| **Documentação arquitetural** | [docs/ARQUITETURA.md](docs/ARQUITETURA.md) |
| **Postman Collection** | [postman/collections/oficina-fluxo-completo.postman_collection.json](postman/collections/oficina-fluxo-completo.postman_collection.json) |

#### Como subir o ambiente

```bash
# 1. Provisionar RDS e EKS (repositórios oficina-infra-db e oficina-infra-k8s)
#    Acionar workflow "Deploy Infraestrutura EKS" → apply no GitHub Actions

# 2. Obter o endereço do LoadBalancer
aws eks update-kubeconfig --region sa-east-1 --name oficina-cluster
kubectl get svc -n oficina

# 3. Acessar a API
http://<EXTERNAL-IP>/swagger-ui.html
```

### Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.5.3 | Framework principal |
| Spring Security + JWT | — | Autenticação stateless |
| Spring Data JPA + Flyway | — | Persistência e migrations |
| PostgreSQL | 16.9 | Banco de dados (RDS) |
| Docker | — | Containerização |
| AWS EKS | 1.32 | Orquestração Kubernetes |
| AWS ECR | — | Registry de imagens |
| GitHub Actions | — | CI/CD |
| New Relic | — | Monitoramento APM e métricas customizadas |
| Micrometer | — | Abstração de métricas (desacoplada do New Relic) |

### Vídeo de demonstração

> Link do vídeo: *(a preencher após gravação)*

---

## Sumário

- [Fase 2 — Arquitetura Hexagonal, Kubernetes e CI/CD](#fase-2--arquitetura-hexagonal-kubernetes-e-cicd)
- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Bounded Contexts implementados](#bounded-contexts-implementados)
- [Como rodar com Docker Compose](#como-rodar-com-docker-compose)
- [Como rodar localmente (sem Docker)](#como-rodar-localmente-sem-docker)
- [Autenticação JWT](#autenticação-jwt)
- [Endpoints disponíveis](#endpoints-disponíveis)
- [Coleção Postman](#coleção-postman)
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

Monolito organizado por **Arquitetura Hexagonal** (Ports & Adapters) e **Bounded Context** (DDD). Cada contexto é um pacote isolado com camadas de domínio, aplicação e adaptadores.

```
br.com.fiap.oficina/
├── atendimento/       ✓ Clientes, Veículos e Atendentes
├── execucao/          ✓ Ordens de Serviço (ciclo completo)
├── estoque/           ✓ Peças, movimentações e reservas
├── administracao/     ✓ Catálogo de Serviços e Relatórios
├── seguranca/         ✓ JWT e Autenticação
└── shared/            ✓ Exceções e tratamento global de erros
```

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.4.5 |
| Spring Data JPA | — |
| Spring Security | 6 |
| jjwt | 0.12.6 |
| SpringDoc OpenAPI | 2.8.4 |
| PostgreSQL | 16 |
| H2 (testes) | — |
| Lombok | — |
| JaCoCo | 0.8.12 |
| Docker / Docker Compose | — |
| Kubernetes | — |
| Kind | — |
| Terraform | >= 1.5 |
| GitHub Actions | — |

---

## Bounded Contexts implementados

### Segurança (JWT)

Autenticação stateless com JWT. Todos os endpoints `/api/admin/**` exigem token válido no header `Authorization: Bearer <token>`.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/admin` | Autenticar administrador com login e senha |

**Configuração via variáveis de ambiente:**

| Variável | Padrão | Descrição |
|---|---|---|
| `OFICINA_ADMIN_LOGIN` | `admin` | Login do usuário padrão criado na inicialização |
| `OFICINA_ADMIN_SENHA` | `admin123` | Senha do usuário padrão |
| `OFICINA_BCRYPT_STRENGTH` | `10` | Fator de custo do BCrypt (4–31) |
| `OFICINA_JWT_SECRET` | *(definido no yml)* | Chave HMAC-SHA para assinar tokens |
| `OFICINA_JWT_EXPIRACAO` | `86400000` | Expiração do token em ms (padrão: 24h) |

**Testes:** `JwtServiceTest` (6) · `AuthControllerTest` (3)

---

### Atendimento ao Cliente

**Value Objects com validação de domínio:**

| Classe | Descrição |
|---|---|
| `Documento` | Encapsula CPF ou CNPJ. Valida pelo algoritmo completo dos dígitos verificadores |
| `Placa` | Valida e normaliza placas no formato antigo (ABC1234) e Mercosul (ABC1D23) |
| `TipoDocumento` | Enum `CPF` / `CNPJ` |

**Entidades:**

| Classe | Descrição |
|---|---|
| `Cliente` | Aggregate Root. Pessoa física (CPF) ou jurídica (CNPJ) |
| `Veiculo` | Associado a um cliente, identificado pela placa |
| `Atendente` | Funcionário responsável pela OS |

**Endpoints** (exigem JWT):

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

POST   /api/admin/atendentes
GET    /api/admin/atendentes
GET    /api/admin/atendentes/{id}
PUT    /api/admin/atendentes/{id}
DELETE /api/admin/atendentes/{id}
```

**Regras de negócio:**
- CPF validado pelo algoritmo dos dois dígitos verificadores
- CNPJ validado pelo algoritmo padrão de 14 dígitos
- Documento duplicado retorna HTTP 422
- Placa duplicada retorna HTTP 422
- Email de atendente duplicado retorna HTTP 422

**Testes:** `ClienteServiceTest` (12) · `VeiculoServiceTest` (9) · `ClienteControllerTest` (10) · `VeiculoControllerTest` (11) · `AtendenteControllerTest` (9)

---

### Execução de Serviços

**Máquina de estados do `StatusOS`:**

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                  ↘         ↘
                                               CANCELADA  CANCELADA
```

Transições são unidirecionais. Qualquer tentativa de transição inválida retorna HTTP 422.

O estado `APROVADO` é atingido quando o cliente aprova o orçamento (endpoint público). O admin então atribui um atendente e chama `iniciar-execucao` para transicionar para `EM_EXECUCAO`.

**Entidades:**

| Classe | Descrição |
|---|---|
| `OrdemDeServico` | Aggregate Root. Controla todo o ciclo de vida |
| `ItemServico` | Serviço do catálogo vinculado à OS |
| `ItemPeca` | Peça vinculada à OS (reserva estoque ao adicionar) |
| `StatusOS` | Enum com validação de transição via `validarTransicaoPara()` |

**Regras de negócio:**
- Número gerado automaticamente no formato `OS-{ano}-{sequencial 5 dígitos}` (ex: `OS-2026-00001`)
- `valorTotal` recalculado automaticamente a cada adição ou remoção de item
- `dataInicioExecucao` registrada ao transicionar para `EM_EXECUCAO`
- `dataFimExecucao` registrada ao transicionar para `FINALIZADA`
- Ao iniciar execução: atendente é atribuído e reservas de peças convertidas em baixas definitivas
- Ao cancelar: todas as reservas de peças são liberadas
- Edição de itens bloqueada a partir de `APROVADO` (inclusive)

**Endpoints admin** (exigem JWT):

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/ordens` | Abrir nova OS |
| `GET` | `/api/admin/ordens` | Listar todas as OSs |
| `GET` | `/api/admin/ordens/{id}` | Buscar OS por ID |
| `POST` | `/api/admin/ordens/{id}/servicos` | Adicionar serviço à OS |
| `DELETE` | `/api/admin/ordens/{id}/servicos/{itemId}` | Remover serviço |
| `POST` | `/api/admin/ordens/{id}/pecas` | Adicionar peça (reserva estoque) |
| `DELETE` | `/api/admin/ordens/{id}/pecas/{itemId}` | Remover peça (libera reserva) |
| `POST` | `/api/admin/ordens/{id}/iniciar-diagnostico` | RECEBIDA → EM_DIAGNOSTICO |
| `POST` | `/api/admin/ordens/{id}/enviar-orcamento` | EM_DIAGNOSTICO → AGUARDANDO_APROVACAO |
| `POST` | `/api/admin/ordens/{id}/iniciar-execucao` | APROVADO → EM_EXECUCAO (requer `{"atendenteId": "<uuid>"}`) |
| `POST` | `/api/admin/ordens/{id}/finalizar` | EM_EXECUCAO → FINALIZADA |
| `POST` | `/api/admin/ordens/{id}/entregar` | FINALIZADA → ENTREGUE |
| `POST` | `/api/admin/ordens/{id}/cancelar` | Cancelar OS |

**Endpoints públicos** (sem autenticação):

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/public/ordens/{numero}/status` | Consultar status por número |
| `POST` | `/api/public/ordens/{numero}/aprovar` | Cliente aprova o orçamento |
| `POST` | `/api/public/ordens/{numero}/recusar` | Cliente recusa o orçamento |

**Métricas de OS (Micrometer — desacopladas do New Relic):**

As métricas são publicadas via `MeterRegistry` do Spring Boot Actuator e coletadas automaticamente pelo New Relic APM. O domínio não depende de nenhuma lib de observabilidade — a porta `MetricasPort` isola essa responsabilidade.

| Métrica | Tipo | Descrição |
|---|---|---|
| `oficina.os.criadas` | Counter | Total de OS abertas |
| `oficina.os.transicoes{status=...}` | Counter | Transições por status (ex: `EM_EXECUCAO`, `FINALIZADA`) |
| `oficina.os.tempo_execucao` | Timer | Tempo entre `EM_EXECUCAO` e `FINALIZADA` |

Acessíveis via `/actuator/metrics/oficina.os.criadas` etc.

**Testes:** `StatusOSTest` (18) · `OrdemDeServicoServiceTest` (16) · `MicrometerMetricasAdapterTest` (3) · `OrdemDeServicoAdminControllerTest` (10) · `OrdemDeServicoPublicControllerTest` (5)

---

### Estoque e Insumos

**Entidades:**

| Classe | Descrição |
|---|---|
| `Peca` | Aggregate Root. Controla `qtdEstoque`, `qtdReservada` e `qtdMinima` |
| `MovimentacaoEstoque` | Auditoria de cada movimentação (ENTRADA, RESERVA, BAIXA, LIBERACAO_RESERVA) |

**Regras de negócio:**
- `qtdDisponivel = qtdEstoque - qtdReservada` (calculado, nunca persistido)
- Reserva verifica disponibilidade — HTTP 422 se insuficiente
- `estoqueCritico = true` na resposta quando `qtdEstoque <= qtdMinima`
- Toda movimentação gera registro em `MovimentacaoEstoque`
- Exclusão bloqueada se houver reservas ativas

**Endpoints** (exigem JWT):

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/pecas` | Cadastrar peça |
| `GET` | `/api/admin/pecas` | Listar todas as peças |
| `GET` | `/api/admin/pecas/criticas` | Listar peças em estoque crítico |
| `GET` | `/api/admin/pecas/{id}` | Buscar peça por ID |
| `PUT` | `/api/admin/pecas/{id}` | Atualizar peça |
| `DELETE` | `/api/admin/pecas/{id}` | Excluir peça |
| `POST` | `/api/admin/pecas/{id}/entrada` | Registrar entrada de estoque |
| `GET` | `/api/admin/pecas/{id}/movimentacoes` | Histórico de movimentações |

**Testes:** `PecaServiceTest` (12) · `PecaControllerTest` (12)

---

### Gestão Administrativa

**Entidades:**

| Classe | Descrição |
|---|---|
| `Servico` | Catálogo de tipos de serviço com nome, descrição e preço base |

**Regras de negócio:**
- Nome de serviço duplicado retorna HTTP 422
- Tempo médio calculado sobre OSs com status `FINALIZADA` ou `ENTREGUE`

**Endpoints** (exigem JWT):

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/admin/servicos` | Cadastrar serviço no catálogo |
| `GET` | `/api/admin/servicos` | Listar todos os serviços |
| `GET` | `/api/admin/servicos/{id}` | Buscar serviço por ID |
| `PUT` | `/api/admin/servicos/{id}` | Atualizar serviço |
| `DELETE` | `/api/admin/servicos/{id}` | Excluir serviço |
| `GET` | `/api/admin/relatorios/tempo-medio` | Tempo médio de execução de OSs |

**Testes:** `ServicoServiceTest` (9) · `ServicoControllerTest` (9) · `RelatorioServiceTest` (4) · `RelatorioControllerTest` (2)

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

## Autenticação JWT

Todos os endpoints `/api/admin/**` exigem autenticação. Obtenha o token via login:

```bash
curl -X POST http://localhost:8080/api/auth/admin \
  -H "Content-Type: application/json" \
  -d '{"login": "admin", "senha": "admin123"}'
```

Resposta:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

Use o token nas requisições protegidas:

```bash
curl http://localhost:8080/api/admin/clientes \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Endpoints disponíveis

### Autenticação

```bash
curl -X POST http://localhost:8080/api/auth/admin \
  -H "Content-Type: application/json" \
  -d '{"login": "admin", "senha": "admin123"}'
```

### Clientes

```bash
# Cadastrar cliente
curl -X POST http://localhost:8080/api/admin/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "nome": "João Silva",
    "email": "joao@email.com",
    "telefone": "11999999999",
    "documento": "529.982.247-25"
  }'
```

### Veículos

```bash
# Cadastrar veículo
curl -X POST http://localhost:8080/api/admin/veiculos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
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

```bash
# Abrir OS
curl -X POST http://localhost:8080/api/admin/ordens \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "clienteId": "<uuid>",
    "veiculoId": "<uuid>",
    "observacoes": "Veículo com barulho no motor"
  }'

# Consultar status (público, sem token)
curl http://localhost:8080/api/public/ordens/OS-2026-00001/status

# Cliente aprovar orçamento (público, sem token)
curl -X POST http://localhost:8080/api/public/ordens/OS-2026-00001/aprovar
```

### Formato de erros

Todos os erros retornam JSON padronizado:

```json
{
  "timestamp": "2026-04-26T15:00:00",
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Já existe um cliente cadastrado com este documento"
}
```

Erros de validação incluem o campo `campos`:

```json
{
  "timestamp": "2026-04-26T15:00:00",
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

## Coleção Postman

A coleção completa está em `postman/collections/oficina-fluxo-completo.postman_collection.json`.

Cobre os 3 fluxos do Miro (Criação da OS, Acompanhamento da OS, Gestão de Peças) com captura automática de variáveis via test scripts (`token`, `osId`, `osNumero`, `servicoId`, `pecaId`, etc.).

**Como usar:**
1. Importe o arquivo no Postman
2. Execute `0.1 Login` — o token é salvo automaticamente
3. Execute o Setup (pastas 1 e 2) para criar serviços e peças
4. Siga os fluxos na ordem das pastas

---

## Documentação interativa (Swagger)

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

Clique em **Authorize** e informe o token JWT para testar os endpoints protegidos diretamente pelo Swagger.

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

| Contexto | Classe de Teste | Tipo | Testes |
|---|---|---|---|
| Segurança | `JwtServiceTest` | Unitário | 6 |
| Segurança | `AuthControllerTest` | MockMvc | 3 |
| Atendimento | `ClienteServiceTest` | Unitário | 12 |
| Atendimento | `VeiculoServiceTest` | Unitário | 9 |
| Atendimento | `ClienteControllerTest` | MockMvc | 10 |
| Atendimento | `VeiculoControllerTest` | MockMvc | 11 |
| Atendimento | `AtendenteControllerTest` | MockMvc | 9 |
| Execução | `StatusOSTest` | Unitário | 18 |
| Execução | `OrdemDeServicoServiceTest` | Unitário | 16 |
| Execução | `MicrometerMetricasAdapterTest` | Unitário | 3 |
| Execução | `OrdemDeServicoAdminControllerTest` | MockMvc | 10 |
| Execução | `OrdemDeServicoPublicControllerTest` | MockMvc | 5 |
| Estoque | `PecaServiceTest` | Unitário | 12 |
| Estoque | `PecaControllerTest` | MockMvc | 12 |
| Administração | `ServicoServiceTest` | Unitário | 9 |
| Administração | `ServicoControllerTest` | MockMvc | 9 |
| Administração | `RelatorioServiceTest` | Unitário | 4 |
| Administração | `RelatorioControllerTest` | MockMvc | 2 |
| Integração | `OficinaApplicationTests` | Spring | 1 |
| **Total** | | | **160** |
