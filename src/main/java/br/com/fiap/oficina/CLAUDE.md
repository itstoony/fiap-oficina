# CLAUDE.md — Sistema de Gestão de Oficina Mecânica
## FIAP Pós-Tech Software Architecture — Tech Challenge Fase 1

---

## CONTEXTO DO PROJETO

Backend MVP de um sistema integrado de atendimento e execução de serviços de uma oficina mecânica de médio porte. Clientes acompanham em tempo real o andamento do serviço e autorizam reparos adicionais via API.

**Documentação DDD completa no Miro:**
https://miro.com/app/board/uXjVGjwS_Y8=/

O board contém:
- Event Storming com 3 fluxos: Criação da OS, Acompanhamento da OS, Gestão de Peças
- Fluxogramas dos processos
- Linguagem Ubíqua completa
- Diagrama de Aggregates, Entities e Value Objects
- Diagrama de Bounded Contexts e relacionamentos

Sempre consultar o Miro antes de implementar qualquer coisa para garantir consistência entre código e documentação.

---

## ESTADO ATUAL

Nenhum bounded context foi implementado ainda. O projeto foi criado com Spring Initializr contendo apenas `spring-boot-starter-web` e `spring-boot-starter-test`. O `pom.xml` precisa ser atualizado com todas as dependências listadas abaixo antes de qualquer implementação.

---

## STACK TECNOLÓGICA

- Java 17
- Spring Boot 3.4.5
- Spring Data JPA + PostgreSQL (produção) / H2 (testes)
- Spring Security + JWT via jjwt 0.12.6
- SpringDoc OpenAPI 2.8.4 (Swagger UI em `/swagger-ui.html`)
- Lombok
- JaCoCo com cobertura mínima de 80% nos domínios críticos
- Maven

**Pacote base:** `br.com.fiap.oficina`

---

## DEPENDÊNCIAS DO POM.XML

Adicionar ao pom.xml além do que já existe:
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `postgresql` (scope runtime)
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` versão 0.12.6
- `springdoc-openapi-starter-webmvc-ui` versão 2.8.4
- `lombok`
- `h2` (scope test)
- `spring-security-test` (scope test)
- Plugin JaCoCo 0.8.12 com mínimo de 80% de cobertura de linhas, excluindo DTOs, configs e exceptions

---

## ARQUITETURA

### Tipo
Monolito em camadas com organização interna por Bounded Context. Cada bounded context é um pacote separado contendo suas próprias camadas (controller, service, repository, domain).

### Estrutura de pacotes
```
br.com.fiap.oficina/
├── atendimento/
│   ├── controller/
│   ├── service/
│   │   └── dto/
│   ├── repository/
│   └── domain/
│       ├── model/
│       └── valueobject/
├── execucao/
│   ├── controller/
│   ├── service/
│   │   └── dto/
│   ├── repository/
│   └── domain/
│       ├── model/
│       └── valueobject/
├── estoque/
│   ├── controller/
│   ├── service/
│   │   └── dto/
│   ├── repository/
│   └── domain/
│       ├── model/
│       └── valueobject/
├── administracao/
│   ├── controller/
│   ├── service/
│   │   └── dto/
│   ├── repository/
│   └── domain/
│       └── model/
├── seguranca/
│   ├── config/
│   ├── controller/
│   ├── filter/
│   ├── service/
│   └── domain/
└── shared/
    └── exception/
```

---

## LINGUAGEM UBÍQUA

Usar sempre estes termos em português no código (classes, métodos, variáveis, tabelas):

| Termo | Descrição |
|---|---|
| `OrdemDeServico` | Aggregate Root principal. Documento central de um atendimento |
| `ItemServico` | Serviço do catálogo vinculado a uma OS |
| `ItemPeca` | Peça ou insumo vinculado a uma OS |
| `Orcamento` | Valor total calculado automaticamente (Σ serviços + Σ peças) |
| `Diagnostico` | Fase de avaliação do veículo pelo atendente |
| `Aprovacao` | Confirmação do cliente para executar os serviços pelo valor orçado |
| `Cliente` | Pessoa física (CPF) ou jurídica (CNPJ) |
| `Veiculo` | Bem associado ao cliente, identificado pela placa |
| `Servico` | Tipo de trabalho do catálogo (ex: alinhamento, troca de óleo) |
| `Peca` | Material ou insumo com controle de estoque |
| `Atendente` | Funcionário responsável por executar os serviços da OS |
| `EstoqueMinimo` | Quantidade mínima aceitável de uma peça antes de gerar alerta |
| `Reserva` | Soft-lock de quantidade ao adicionar peça à OS antes da aprovação |
| `Baixa` | Débito definitivo do estoque após aprovação da OS |

---

## BOUNDED CONTEXTS

### 1. Atendimento ao Cliente
**Responsabilidade:** Identificação e cadastro de clientes e veículos. Consulta pública de OS.

**Aggregates e Entities:**
- `Cliente` — Aggregate Root. Pessoa física (CPF) ou jurídica (CNPJ)
- `Veiculo` — Entity associada ao Cliente
- `Atendente` — Entity

**Value Objects:**
- `Documento` — encapsula CPF ou CNPJ. Factory method `Documento.of(String)`. Valida pelo algoritmo completo dos dígitos verificadores. Aceita com ou sem formatação
- `Placa` — encapsula placa. Factory method `Placa.of(String)`. Valida formato antigo (ABC1234) e Mercosul (ABC1D23). Normaliza para maiúsculas
- `TipoDocumento` — enum CPF / CNPJ

**Regras de negócio:**
- CPF deve passar no algoritmo dos dois dígitos verificadores
- CNPJ deve passar no algoritmo padrão de 14 dígitos
- Não permitir dois clientes com o mesmo CPF/CNPJ
- Não permitir dois veículos com a mesma placa

**Endpoints (todos requerem JWT):**
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

---

### 2. Execução de Serviços
**Responsabilidade:** Ciclo de vida completo da Ordem de Serviço.

**Aggregates e Entities:**
- `OrdemDeServico` — Aggregate Root
- `ItemServico` — Entity filha da OS
- `ItemPeca` — Entity filha da OS

**Value Objects:**
- `StatusOS` — enum com validação de transição unidirecional

**Máquina de estados do StatusOS:**
```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                  ↘
                                                  CANCELADA (permitido antes de EM_EXECUCAO)
```

**Regras de negócio:**
- Transições de status são unidirecionais — nunca retroceder
- `valorTotal` = Σ(itemServico.quantidade × precoUnitario) + Σ(itemPeca.quantidade × precoUnitario)
- Recalcular `valorTotal` automaticamente a cada adição ou remoção de item
- Ao mudar para `EM_EXECUCAO`: chamar EstoqueService para converter reservas em baixas definitivas
- Ao `CANCELAR`: chamar EstoqueService para liberar todas as reservas da OS
- Registrar `dataInicioExecucao` ao entrar em `EM_EXECUCAO`
- Registrar `dataFimExecucao` ao entrar em `FINALIZADA`
- Número da OS gerado automaticamente e único (formato: OS-{ano}-{sequencial 5 dígitos})

**Endpoints:**
```
# Admin (requerem JWT)
POST   /api/admin/ordens
GET    /api/admin/ordens
GET    /api/admin/ordens/{id}
POST   /api/admin/ordens/{id}/servicos
DELETE /api/admin/ordens/{id}/servicos/{itemId}
POST   /api/admin/ordens/{id}/pecas
DELETE /api/admin/ordens/{id}/pecas/{itemId}
POST   /api/admin/ordens/{id}/iniciar-diagnostico
POST   /api/admin/ordens/{id}/enviar-orcamento
POST   /api/admin/ordens/{id}/iniciar-execucao
POST   /api/admin/ordens/{id}/finalizar
POST   /api/admin/ordens/{id}/entregar
POST   /api/admin/ordens/{id}/cancelar

# Público (sem JWT)
GET    /api/public/ordens/{numero}/status
POST   /api/public/ordens/{numero}/aprovar
POST   /api/public/ordens/{numero}/recusar
```

---

### 3. Estoque e Insumos
**Responsabilidade:** Gestão de peças, controle de estoque, reservas e baixas.

**Aggregates e Entities:**
- `Peca` — Aggregate Root
- `MovimentacaoEstoque` — Entity de auditoria

**Enum:**
- `TipoMovimentacao` — ENTRADA, RESERVA, BAIXA, LIBERACAO_RESERVA

**Regras de negócio:**
- `qtdDisponivel` = `qtdEstoque` - `qtdReservada` (calcular, nunca persistir)
- Ao reservar: verificar `qtdDisponivel >= qtdSolicitada`. Se não, lançar `RegraDeNegocioException` HTTP 422
- `qtdEstoque` e `qtdReservada` nunca podem ser negativos
- Se `qtdEstoque <= qtdMinima` após qualquer operação: flag `estoqueCritico = true` na resposta
- Toda movimentação gera registro em `MovimentacaoEstoque` com: data, tipo, quantidade, osId (nullable)

**Endpoints (todos requerem JWT):**
```
POST   /api/admin/pecas
GET    /api/admin/pecas
GET    /api/admin/pecas/{id}
PUT    /api/admin/pecas/{id}
DELETE /api/admin/pecas/{id}
POST   /api/admin/pecas/{id}/entrada
GET    /api/admin/pecas/criticas
GET    /api/admin/pecas/{id}/movimentacoes
```

---

### 4. Gestão Administrativa
**Responsabilidade:** CRUD do catálogo de serviços e monitoramento de tempo médio de execução.

**Entities:**
- `Servico` — catálogo de tipos de serviço com nome, descrição e preço base

**Regras de negócio:**
- Tempo médio = média de `(dataFimExecucao - dataInicioExecucao)` calculada sobre OSs com status `FINALIZADA` ou `ENTREGUE`

**Endpoints (todos requerem JWT):**
```
POST   /api/admin/servicos
GET    /api/admin/servicos
GET    /api/admin/servicos/{id}
PUT    /api/admin/servicos/{id}
DELETE /api/admin/servicos/{id}
GET    /api/admin/relatorios/tempo-medio
```

---

### 5. Segurança (JWT)
**Responsabilidade:** Autenticação e autorização dos endpoints administrativos.

**Regras:**
- `/api/admin/**` — requer JWT válido no header `Authorization: Bearer <token>`
- `/api/public/**` — público, sem autenticação
- `/api/auth/login` — público
- `/swagger-ui.html` e `/api-docs/**` — públicos
- JWT gerado com jjwt 0.12.6
- Secret e expiração configuráveis via `application.properties` com prefixo `oficina.jwt`

**Endpoint:**
```
POST /api/auth/login   — body: { "login": "...", "senha": "..." }
                       — retorna: { "token": "..." }
```

---

## REGRAS DE CÓDIGO

1. **Lombok** em todas as entidades: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`. Nunca usar `@Data` em entidades JPA
2. **UUID** como tipo de ID em todas as entidades com `@GeneratedValue(strategy = GenerationType.UUID)`
3. **`@Transactional(readOnly = true)`** em todos os métodos de leitura nos Services
4. **`@Transactional`** em todos os métodos de escrita nos Services
5. **DTOs como records** dentro de uma classe wrapper (padrão: `XxxDTO.CadastrarRequest`, `XxxDTO.AtualizarRequest`, `XxxDTO.Response`)
6. **Nunca expor entidades** nos Controllers — sempre mapear para DTO de Response
7. **Value Objects** são `@Embeddable` com construtor `protected` sem argumentos e factory method estático `of()`
8. **Validação** com anotações `jakarta.validation` nos DTOs — nunca no Service
9. **Exceções** em `shared/exception/`: `RecursoNaoEncontradoException` (404) e `RegraDeNegocioException` (422)
10. **`GlobalExceptionHandler`** com `@RestControllerAdvice` retornando JSON padronizado para todos os erros
11. **`@PrePersist`** e **`@PreUpdate`** em todas as entidades para `criadoEm` e `atualizadoEm`
12. **Testes unitários** com JUnit 5 + Mockito para Services, AssertJ para assertions
13. **Testes de integração** com `@SpringBootTest` + MockMvc para os Controllers
14. **Swagger** em todos os Controllers com `@Tag`, `@Operation`, e `@SecurityRequirement(name = "bearerAuth")` nos endpoints admin

---

## CONFIGURAÇÕES

### src/main/resources/application.properties
```properties
spring.application.name=oficina
spring.datasource.url=jdbc:postgresql://localhost:5432/oficina
spring.datasource.username=oficina
spring.datasource.password=oficina
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
oficina.jwt.secret=chave-secreta-minimo-256-bits-para-o-jwt-do-sistema-oficina
oficina.jwt.expiracao=86400000
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

### src/test/resources/application-test.properties
```properties
spring.datasource.url=jdbc:h2:mem:oficina_test;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

---

## DOCKER

### Dockerfile (na raiz do projeto)
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/oficina-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml (na raiz do projeto)
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/oficina
      SPRING_DATASOURCE_USERNAME: oficina
      SPRING_DATASOURCE_PASSWORD: oficina
    depends_on:
      db:
        condition: service_healthy
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: oficina
      POSTGRES_USER: oficina
      POSTGRES_PASSWORD: oficina
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U oficina"]
      interval: 10s
      timeout: 5s
      retries: 5
volumes:
  postgres_data:
```

---

## ORDEM DE IMPLEMENTAÇÃO

1. 🔲 Atualizar `pom.xml` com todas as dependências
2. 🔲 Criar `application.properties` e `application-test.properties`
3. 🔲 **Shared** — `RecursoNaoEncontradoException`, `RegraDeNegocioException`, `GlobalExceptionHandler`
4. 🔲 **Segurança** — JWT filter, SecurityConfig, UserDetails, AuthController (fazer primeiro para proteger os outros endpoints)
5. 🔲 **Atendimento** — Cliente, Veiculo, Atendente com Value Objects Documento e Placa
6. 🔲 **Estoque** — Peca, MovimentacaoEstoque (fazer antes de Execução pois OS depende de Peca)
7. 🔲 **Execução** — OrdemDeServico, ItemServico, ItemPeca, StatusOS
8. 🔲 **Administração** — Servico, relatório de tempo médio
9. 🔲 **Testes de integração** com MockMvc
10. 🔲 **Dockerfile** e **docker-compose.yml**
11. 🔲 **Análise de vulnerabilidades** com OWASP Dependency-Check ou Trivy