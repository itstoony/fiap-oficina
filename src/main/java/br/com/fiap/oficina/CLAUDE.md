# CLAUDE.md — Sistema de Gestão de Oficina Mecânica
## FIAP Pós-Tech Software Architecture — Tech Challenge Fase 1 + Fase 2

---

## ⚠️ FASE 2 — INCREMENTOS (implementar sobre a Fase 1 já concluída)

A Fase 1 está **completamente implementada** com 157 testes. Não refazer o que já existe.
Os passos abaixo são os únicos incrementos exigidos pela Fase 2.
Implementar na ordem indicada.

---

### PASSO 1 — Refatorar para Arquitetura Hexagonal

Reorganizar cada bounded context para o padrão Ports & Adapters. Fazer um contexto por vez: `execucao` → `atendimento` → `estoque` → `administracao` → `seguranca`.

**Nova estrutura de cada contexto:**
```
{contexto}/
├── application/
│   ├── port/
│   │   ├── in/           ← interfaces dos Use Cases
│   │   └── out/          ← interfaces de saída (repositórios, email)
│   └── service/          ← implementação dos Use Cases
├── domain/
│   ├── model/            ← zero dependência de Spring/JPA/Lombok
│   └── valueobject/
└── adapter/
    ├── in/
    │   └── web/          ← Controllers REST
    └── out/
        ├── persistence/  ← implementação JPA dos ports de saída
        └── email/        ← adapter de email (só no contexto execucao)
```

**Regras obrigatórias:**
- `domain` — zero imports de Spring, JPA ou qualquer framework. Java puro.
- `application/service` — depende apenas de `domain` e das interfaces `port/in` e `port/out`. Nunca importa classes de `adapter`.
- `adapter` — depende de `application` e `domain`. Nunca é importado por `application`.
- Controllers injetam interfaces `port/in`, nunca Services diretamente.
- Repositories JPA implementam interfaces `port/out`.

**Exemplo para o contexto `execucao`:**
```java
// application/port/in/AbrirOrdemDeServicoUseCase.java
public interface AbrirOrdemDeServicoUseCase {
    OrdemDeServicoResponse abrir(AbrirOrdemDeServicoCommand command);
}

// application/port/out/OrdemDeServicoRepositoryPort.java
public interface OrdemDeServicoRepositoryPort {
    OrdemDeServico salvar(OrdemDeServico os);
    Optional<OrdemDeServico> buscarPorId(UUID id);
    Optional<OrdemDeServico> buscarPorNumero(String numero);
    List<OrdemDeServico> listarAtivasOrdenadas();
}

// application/service/AbrirOrdemDeServicoService.java
@Service
@RequiredArgsConstructor
public class AbrirOrdemDeServicoService implements AbrirOrdemDeServicoUseCase {
    private final OrdemDeServicoRepositoryPort repositorio;
    // lógica de negócio aqui
}

// adapter/in/web/OrdemDeServicoAdminController.java
@RestController
@RequiredArgsConstructor
public class OrdemDeServicoAdminController {
    private final AbrirOrdemDeServicoUseCase abrirOrdemDeServicoUseCase;
    // chama o use case, nunca o service diretamente
}

// adapter/out/persistence/OrdemDeServicoRepositoryAdapter.java
@Component
@RequiredArgsConstructor
public class OrdemDeServicoRepositoryAdapter implements OrdemDeServicoRepositoryPort {
    private final OrdemDeServicoJpaRepository jpaRepository;
}
```

**Clean Code — aplicar durante a refatoração:**
- Nenhum método com mais de 20 linhas — extrair em métodos privados com nomes descritivos
- Sem variáveis abreviadas sem contexto (`os` → `ordemDeServico`, `qtd` → `quantidade`)
- Sem comentários que apenas repetem o código
- Constantes nomeadas em vez de números mágicos
- Cada classe com responsabilidade única

---

### PASSO 2 — Alterar listagem de OSs e exclusão lógica

**2.1** Adicionar campo `ativo` na entidade `OrdemDeServico` (domínio):
```java
private boolean ativo = true;
```
Ao transicionar para `FINALIZADA`, `ENTREGUE` ou `CANCELADA`, setar `ativo = false` automaticamente dentro do método de transição de status da própria entidade.

**2.2** Implementar nova query de listagem no adapter de persistência:
```java
// Ordenação obrigatória: EM_EXECUCAO > AGUARDANDO_APROVACAO > APROVADO > EM_DIAGNOSTICO > RECEBIDA
// Dentro do mesmo status: mais antigas primeiro
@Query("""
    SELECT o FROM OrdemDeServico o
    WHERE o.ativo = true
    ORDER BY
      CASE o.status
        WHEN 'EM_EXECUCAO'           THEN 1
        WHEN 'AGUARDANDO_APROVACAO'  THEN 2
        WHEN 'APROVADO'              THEN 3
        WHEN 'EM_DIAGNOSTICO'        THEN 4
        WHEN 'RECEBIDA'              THEN 5
        ELSE 6
      END ASC,
      o.dataAbertura ASC
    """)
List<OrdemDeServico> findAllAtivasOrdenadas();
```

**2.3** Comportamento:
- `GET /api/admin/ordens` — retorna apenas OSs com `ativo = true` na ordem acima
- `GET /api/admin/ordens/{id}` — continua retornando qualquer OS incluindo finalizadas/entregues

---

### PASSO 3 — Notificação por email ao enviar orçamento

**3.1** Adicionar dependências no pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**3.2** Criar port de saída:
```java
// execucao/application/port/out/NotificacaoEmailPort.java
public interface NotificacaoEmailPort {
    void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal);
    void enviarConfirmacaoAprovacao(String emailCliente, String numeroOS);
    void enviarConfirmacaoRecusa(String emailCliente, String numeroOS);
}
```

**3.3** Adapter real (produção — profile `prod`):
```java
// execucao/adapter/out/email/EmailNotificacaoAdapter.java
@Component
@Profile("prod")
@RequiredArgsConstructor
public class EmailNotificacaoAdapter implements NotificacaoEmailPort {
    private final JavaMailSender mailSender;

    @Value("${oficina.app.url}")
    private String appUrl;

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailCliente);
        msg.setSubject("Orçamento disponível — OS " + numeroOS);
        msg.setText("""
            Olá! Seu orçamento está pronto.
            OS: %s | Valor: R$ %s
            Aprovar: %s/api/public/ordens/%s/aprovar
            Recusar: %s/api/public/ordens/%s/recusar
            """.formatted(numeroOS, valorTotal, appUrl, numeroOS, appUrl, numeroOS));
        mailSender.send(msg);
    }
}
```

**3.4** Adapter mock (desenvolvimento — profile `dev`):
```java
// execucao/adapter/out/email/MockEmailNotificacaoAdapter.java
@Component
@Profile("dev")
@Slf4j
public class MockEmailNotificacaoAdapter implements NotificacaoEmailPort {
    @Value("${oficina.app.url:http://localhost:8080}")
    private String appUrl;

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal) {
        log.info("[EMAIL MOCK] Para: {} | OS: {} | Valor: R$ {} | Aprovar: {}/api/public/ordens/{}/aprovar",
            emailCliente, numeroOS, valorTotal, appUrl, numeroOS);
    }
}
```

**3.5** Disparar email no service ao mudar status para `AGUARDANDO_APROVACAO`:
```java
notificacaoEmailPort.enviarOrcamentoParaAprovacao(
    ordemDeServico.getCliente().getEmail(),
    ordemDeServico.getNumero(),
    ordemDeServico.getValorTotal()
);
```

**3.6** Adicionar ao application.properties:
```properties
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
oficina.app.url=${APP_URL:http://localhost:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

**3.7** Liberar actuator no SecurityConfig:
```java
.requestMatchers("/actuator/health").permitAll()
```

---

### PASSO 4 — Manifestos Kubernetes em /k8s

Criar a pasta `/k8s` na raiz com os arquivos abaixo:

**k8s/namespace.yaml**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: oficina
```

**k8s/secret.yaml**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: oficina-secrets
  namespace: oficina
type: Opaque
stringData:
  db-password: "oficina"
  jwt-secret: "chave-secreta-minimo-256-bits-para-o-jwt-do-sistema-oficina"
  mail-username: ""
  mail-password: ""
```

**k8s/configmap.yaml**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: oficina-config
  namespace: oficina
data:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://oficina-db:5432/oficina"
  SPRING_DATASOURCE_USERNAME: "oficina"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"
  SPRING_PROFILES_ACTIVE: "prod"
  MAIL_HOST: "smtp.gmail.com"
  MAIL_PORT: "587"
  APP_URL: "http://oficina-app:8080"
```

**k8s/db-deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oficina-db
  namespace: oficina
spec:
  replicas: 1
  selector:
    matchLabels:
      app: oficina-db
  template:
    metadata:
      labels:
        app: oficina-db
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          env:
            - name: POSTGRES_DB
              value: oficina
            - name: POSTGRES_USER
              value: oficina
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: oficina-secrets
                  key: db-password
          ports:
            - containerPort: 5432
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              name: db-data
      volumes:
        - name: db-data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: oficina-db
  namespace: oficina
spec:
  selector:
    app: oficina-db
  ports:
    - port: 5432
      targetPort: 5432
```

**k8s/app-deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oficina-app
  namespace: oficina
spec:
  replicas: 2
  selector:
    matchLabels:
      app: oficina-app
  template:
    metadata:
      labels:
        app: oficina-app
    spec:
      containers:
        - name: oficina
          image: itstoony/oficina:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: oficina-config
          env:
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: oficina-secrets
                  key: db-password
            - name: OFICINA_JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: oficina-secrets
                  key: jwt-secret
            - name: MAIL_USERNAME
              valueFrom:
                secretKeyRef:
                  name: oficina-secrets
                  key: mail-username
            - name: MAIL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: oficina-secrets
                  key: mail-password
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: oficina-app
  namespace: oficina
spec:
  selector:
    app: oficina-app
  ports:
    - port: 8080
      targetPort: 8080
  type: LoadBalancer
```

**k8s/hpa.yaml**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: oficina-hpa
  namespace: oficina
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: oficina-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

### PASSO 5 — Scripts Terraform em /infra

Criar a pasta `/infra` na raiz com os arquivos abaixo. Usar **Kind** para cluster local.

**infra/main.tf**
```hcl
terraform {
  required_version = ">= 1.5"
  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.2"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

provider "kind" {}

resource "kind_cluster" "oficina" {
  name = var.cluster_name
  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"
    node { role = "control-plane" }
    node { role = "worker" }
    node { role = "worker" }
  }
}

provider "kubernetes" {
  host                   = kind_cluster.oficina.endpoint
  client_certificate     = kind_cluster.oficina.client_certificate
  client_key             = kind_cluster.oficina.client_key
  cluster_ca_certificate = kind_cluster.oficina.cluster_ca_certificate
}

resource "kubernetes_namespace" "oficina" {
  metadata { name = "oficina" }
  depends_on = [kind_cluster.oficina]
}
```

**infra/variables.tf**
```hcl
variable "cluster_name" {
  description = "Nome do cluster Kind"
  type        = string
  default     = "oficina-cluster"
}

variable "db_password" {
  description = "Senha do PostgreSQL"
  type        = string
  sensitive   = true
  default     = "oficina"
}

variable "jwt_secret" {
  description = "Secret do JWT"
  type        = string
  sensitive   = true
  default     = "chave-secreta-minimo-256-bits-para-o-jwt-do-sistema-oficina"
}
```

**infra/outputs.tf**
```hcl
output "cluster_name" {
  description = "Nome do cluster criado"
  value       = kind_cluster.oficina.name
}

output "cluster_endpoint" {
  description = "Endpoint do cluster Kubernetes"
  value       = kind_cluster.oficina.endpoint
}
```

**infra/kubernetes.tf**
```hcl
resource "kubernetes_secret" "oficina" {
  metadata {
    name      = "oficina-secrets"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }
  data = {
    "db-password"   = var.db_password
    "jwt-secret"    = var.jwt_secret
    "mail-username" = ""
    "mail-password" = ""
  }
  depends_on = [kubernetes_namespace.oficina]
}
```

**infra/README.md**
```markdown
# Infraestrutura — Oficina Mecânica (Fase 2)

## Recursos criados
- Cluster Kind com 1 control-plane e 2 workers
- Namespace `oficina` no Kubernetes
- Secret com credenciais do banco, JWT e email

## Pré-requisitos
- Terraform >= 1.5
- Docker
- Kind: https://kind.sigs.k8s.io/
- kubectl

## Como provisionar
  cd infra
  terraform init
  terraform apply
  kind get kubeconfig --name oficina-cluster > ~/.kube/config
  kubectl apply -f ../k8s/
  kubectl get pods -n oficina

## Como destruir
  terraform destroy
```

---

### PASSO 6 — Pipeline CI/CD com GitHub Actions

Criar `.github/workflows/ci-cd.yml`:

```yaml
name: CI/CD — Oficina Mecânica

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  DOCKER_IMAGE: itstoony/oficina
  JAVA_VERSION: '17'

jobs:

  build-and-test:
    name: Build e Testes
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven
      - name: Build e Testes com cobertura
        run: ./mvnw clean verify
      - name: Upload relatório JaCoCo
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: target/site/jacoco/

  docker:
    name: Build e Push Docker
    runs-on: ubuntu-latest
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven
      - run: ./mvnw clean package -DskipTests
      - uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.DOCKER_IMAGE }}:latest
            ${{ env.DOCKER_IMAGE }}:${{ github.sha }}

  deploy:
    name: Deploy Kubernetes
    runs-on: ubuntu-latest
    needs: docker
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - uses: azure/setup-kubectl@v3
      - name: Configurar kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBECONFIG }}" > ~/.kube/config
      - name: Atualizar tag da imagem
        run: |
          sed -i "s|itstoony/oficina:latest|${{ env.DOCKER_IMAGE }}:${{ github.sha }}|g" k8s/app-deployment.yaml
      - name: Deploy banco
        run: |
          kubectl apply -f k8s/namespace.yaml
          kubectl apply -f k8s/secret.yaml
          kubectl apply -f k8s/configmap.yaml
          kubectl apply -f k8s/db-deployment.yaml
          kubectl rollout status deployment/oficina-db -n oficina --timeout=120s
      - name: Deploy aplicação
        run: |
          kubectl apply -f k8s/app-deployment.yaml
          kubectl apply -f k8s/hpa.yaml
          kubectl rollout status deployment/oficina-app -n oficina --timeout=180s
      - run: kubectl get pods -n oficina
```

**Secrets a configurar no GitHub (Settings → Secrets and variables → Actions):**
- `DOCKER_USERNAME` — usuário DockerHub (itstoony)
- `DOCKER_PASSWORD` — token de acesso do DockerHub (gerar em hub.docker.com/settings/security)
- `KUBECONFIG` — conteúdo raw do `~/.kube/config` após provisionar o cluster

---

### PASSO 7 — Atualizar Dockerfile para multi-stage

Substituir o Dockerfile existente:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/oficina-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

---

### PASSO 8 — Atualizar README.md

Manter todo o conteúdo existente do README da Fase 1 e adicionar seção de Fase 2 no topo com:
- Descrição dos objetivos da Fase 2
- Diagrama da arquitetura em ASCII (abaixo)
- Instruções de deploy em Kubernetes
- Instruções de provisionamento com Terraform
- Link para o vídeo (preencher após gravar)

**Diagrama para o README:**
```
┌─────────────── GitHub Actions CI/CD ──────────────────┐
│  Push → Build → Test → Docker Push → kubectl apply    │
└───────────────────────────────────────────────────┬───┘
                                                    │
┌──────────────── Cluster Kubernetes (Kind) ─────────▼──┐
│                                                        │
│  ┌─────────────────┐        ┌──────────────────┐      │
│  │  oficina-app    │◄──────►│  oficina-db      │      │
│  │  (2-10 pods)    │        │  (PostgreSQL 16) │      │
│  │  HPA: CPU > 70% │        │                  │      │
│  └────────┬────────┘        └──────────────────┘      │
│           │ ConfigMap + Secrets                        │
│  ┌────────▼────────┐                                   │
│  │  LoadBalancer   │                                   │
│  │  :8080          │                                   │
│  └─────────────────┘                                   │
└────────────────────────────────────────────────────────┘
         │
         ▼
   Cliente / Swagger / Postman
```

---

### O QUE VOCÊ FAZ MANUALMENTE (não é para o Claude Code)

- **Gravar o vídeo** (até 15 min) mostrando: deploy, pipeline CI/CD, consumo das APIs e escalabilidade (HPA criando pods com `kubectl get pods -n oficina -w`)
- **Configurar secrets** no GitHub Actions (DOCKER_USERNAME, DOCKER_PASSWORD, KUBECONFIG)
- **Instalar Kind e Terraform** localmente: `brew install kind terraform kubectl`
- **Montar o PDF de entrega** com: link do repositório, desenho da arquitetura e link do vídeo
- **Dar acesso ao usuário** `soat-architecture` no repositório (diferente da Fase 1 que era `soatarchitecture`)

---

---

## CONTEXTO COMPLETO DA FASE 1 (referência — não reimplementar)

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

---

## ESTADO ATUAL

Fase 1 **completamente implementada** com 157 testes. Pacote base: `br.com.fiap.oficina`.

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

## ARQUITETURA ATUAL (Fase 1 — será refatorada para Hexagonal no Passo 1)

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

**Aggregates e Entities:** `Cliente` (Aggregate Root), `Veiculo`, `Atendente`

**Value Objects:**
- `Documento` — CPF ou CNPJ com validação pelo algoritmo dos dígitos verificadores. Factory method `Documento.of(String)`
- `Placa` — formato antigo (ABC1234) e Mercosul (ABC1D23). Factory method `Placa.of(String)`
- `TipoDocumento` — enum CPF / CNPJ

**Regras de negócio:**
- CPF deve passar no algoritmo dos dois dígitos verificadores
- CNPJ deve passar no algoritmo padrão de 14 dígitos
- Não permitir dois clientes com o mesmo CPF/CNPJ
- Não permitir dois veículos com a mesma placa

**Endpoints (todos requerem JWT):**
```
POST/GET/PUT/DELETE /api/admin/clientes
GET /api/admin/clientes/documento/{documento}
POST/GET/PUT/DELETE /api/admin/veiculos
GET /api/admin/veiculos/placa/{placa}
GET /api/admin/veiculos/cliente/{clienteId}
```

---

### 2. Execução de Serviços
**Responsabilidade:** Ciclo de vida completo da Ordem de Serviço.

**Aggregates e Entities:** `OrdemDeServico` (Aggregate Root), `ItemServico`, `ItemPeca`

**Value Objects:** `StatusOS` — enum com validação de transição unidirecional

**Máquina de estados:**
```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                  ↘ CANCELADA (antes de EM_EXECUCAO)
```

**Regras de negócio:**
- Transições unidirecionais — nunca retroceder
- `valorTotal` = Σ(serviços) + Σ(peças) — recalculado a cada alteração
- Ao `EM_EXECUCAO`: baixar peças reservadas do estoque
- Ao `CANCELAR`: liberar peças reservadas
- Número da OS: `OS-{ano}-{sequencial 5 dígitos}`

**Endpoints admin (JWT) e públicos (sem JWT):**
```
POST/GET /api/admin/ordens
GET /api/admin/ordens/{id}
POST /api/admin/ordens/{id}/servicos
POST /api/admin/ordens/{id}/pecas
POST /api/admin/ordens/{id}/iniciar-diagnostico
POST /api/admin/ordens/{id}/enviar-orcamento
POST /api/admin/ordens/{id}/iniciar-execucao
POST /api/admin/ordens/{id}/finalizar
POST /api/admin/ordens/{id}/entregar
POST /api/admin/ordens/{id}/cancelar
GET  /api/public/ordens/{numero}/status
POST /api/public/ordens/{numero}/aprovar
POST /api/public/ordens/{numero}/recusar
```

---

### 3. Estoque e Insumos
**Responsabilidade:** Gestão de peças, reservas e baixas.

**Aggregates e Entities:** `Peca` (Aggregate Root), `MovimentacaoEstoque`

**Enum:** `TipoMovimentacao` — ENTRADA, RESERVA, BAIXA, LIBERACAO_RESERVA

**Regras:**
- `qtdDisponivel` = `qtdEstoque` - `qtdReservada` (calculado, nunca persistido)
- Reserva verifica disponibilidade — HTTP 422 se insuficiente
- `estoqueCritico = true` quando `qtdEstoque <= qtdMinima`
- Toda movimentação gera registro em `MovimentacaoEstoque`

---

### 4. Gestão Administrativa
**Responsabilidade:** CRUD do catálogo de serviços e tempo médio de execução.

**Entities:** `Servico`

**Endpoints:** CRUD em `/api/admin/servicos` + `GET /api/admin/relatorios/tempo-medio`

---

### 5. Segurança (JWT)
- `/api/admin/**` — requer JWT
- `/api/public/**` — público
- `/api/auth/login` — público
- `/swagger-ui.html` e `/api-docs/**` — públicos
- JWT com jjwt 0.12.6, secret e expiração via application.properties

---

## REGRAS DE CÓDIGO

1. Lombok em entidades: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`. Nunca `@Data` em entidades JPA
2. UUID como ID com `@GeneratedValue(strategy = GenerationType.UUID)`
3. `@Transactional(readOnly = true)` em leituras, `@Transactional` em escritas
4. DTOs como records dentro de classe wrapper (`XxxDTO.CadastrarRequest`, `XxxDTO.Response`)
5. Nunca expor entidades nos Controllers — sempre mapear para DTO
6. Value Objects são `@Embeddable` com construtor `protected` e factory method `of()`
7. Validação com `jakarta.validation` nos DTOs — nunca no Service
8. Exceções em `shared/exception/`: `RecursoNaoEncontradoException` (404) e `RegraDeNegocioException` (422)
9. `GlobalExceptionHandler` com `@RestControllerAdvice`
10. `@PrePersist` e `@PreUpdate` em todas as entidades
11. Testes: JUnit 5 + Mockito + AssertJ para unitários, MockMvc para integração
12. Swagger com `@Tag`, `@Operation` e `@SecurityRequirement(name = "bearerAuth")` nos endpoints admin

---

## CONFIGURAÇÕES

### application.properties
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
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
oficina.app.url=${APP_URL:http://localhost:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

### application-test.properties
```properties
spring.datasource.url=jdbc:h2:mem:oficina_test;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

---

## DOCKER

### Dockerfile (multi-stage — atualizado na Fase 2)
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/oficina-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

### docker-compose.yml (desenvolvimento local)
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
      SPRING_PROFILES_ACTIVE: dev
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