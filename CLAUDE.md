# CLAUDE.md — Tech Challenge Fase 3
## FIAP Pós-Tech Software Architecture

---

## CONFIGURAÇÃO INICIAL — OBRIGATÓRIO

Antes de executar qualquer tarefa, faça as seguintes perguntas ao usuário e aguarde as respostas:

1. **"Qual é o seu AWS Account ID?"**
   → Substituir AWS_ACCOUNT_ID por esse valor em todo o arquivo
   → Usado em: nome do bucket S3, ARNs, tags dos recursos

2. **"Qual é a região AWS?"** (pressione Enter para usar o padrão sa-east-1)
   → Substituir AWS_REGION por esse valor em todo o arquivo

Confirme os valores recebidos antes de prosseguir:
- "Entendido. Vou usar Account ID: [valor] e região: [valor]. Podemos começar?"

Não execute nenhum comando nem crie nenhum arquivo antes dessa confirmação.

---

## FIAP Pós-Tech Software Architecture

---

## CONTEXTO GERAL

Evolução da aplicação de oficina mecânica para nível corporativo com cloud AWS, Function Serverless, API Gateway, banco gerenciado, monitoramento e 4 repositórios separados.

**Repositório atual (este):** ver qual dos 4 está sendo trabalhado — checar o nome do diretório.

**Os 4 repositórios da Fase 3:**
- `oficina-lambda` — Function Serverless de autenticação por CPF
- `oficina-infra-k8s` — Terraform para cluster Kubernetes no EKS
- `oficina-infra-db` — Terraform para banco RDS PostgreSQL
- `oficina-app` — Aplicação Spring Boot (evolução da Fase 2)

**Cloud:** AWS
**Região:** sa-east-1 (São Paulo)
**Account ID:** AWS_ACCOUNT_ID

**Contexto das fases anteriores:**
- Fase 1: monolito Spring Boot com DDD, 5 bounded contexts, JWT, PostgreSQL, Docker
- Fase 2: Arquitetura Hexagonal, Kubernetes com Kind (local), Terraform Kind, CI/CD GitHub Actions
- Fase 3: tudo na AWS, 4 repos separados, Lambda, API Gateway, RDS, EKS, Datadog

---

## ESTADO ATUAL (O QUE JÁ EXISTE)

### Aplicação Spring Boot (oficina-app)
- Java 17, Spring Boot 3.4.5
- Arquitetura Hexagonal (Ports and Adapters)
- 5 bounded contexts: atendimento, execucao, estoque, administracao, seguranca
- 157 testes automatizados (JUnit 5 + Mockito + MockMvc)
- JWT com jjwt 0.12.6 — autenticação atual por login/senha
- Flyway para migrações de banco
- Spring Actuator em /actuator/health
- Dockerfile multi-stage

### Infraestrutura existente (Fase 2)
- Manifestos Kubernetes em /k8s (namespace, secret, configmap, deployments, HPA)
- Terraform com provider Kind (local) em /infra
- CI/CD GitHub Actions com 3 jobs: build+test, docker, deploy

---

## O QUE A FASE 3 EXIGE

### 1. Autenticação e API Gateway
- Implementar AWS API Gateway na frente de toda a aplicação
- Substituir autenticação por login/senha por autenticação via CPF
- Lambda em Python que: valida CPF, consulta o cliente no banco, gera JWT
- Rotas /api/admin/** protegidas pelo API Gateway (exigem JWT da Lambda)
- Rotas /api/public/** liberadas sem autenticação

### 2. Estrutura de Repositórios e CI/CD
- 4 repositórios separados, cada um com GitHub Actions
- Branch main protegida em todos (sem commits diretos, PRs obrigatórios)
- Deploy automático para AWS

### 3. Infraestrutura AWS com Terraform
- EKS para o cluster Kubernetes
- RDS PostgreSQL para o banco gerenciado
- API Gateway + Lambda integrados
- Tudo provisionado por Terraform

### 4. Monitoramento
- Datadog integrado
- Métricas: latência, CPU/memória K8s, healthchecks, alertas de OS
- Logs estruturados em JSON com correlation ID
- Dashboards: volume de OSs, tempo médio por status, erros

### 5. Documentação
- Diagrama de Componentes (cloud)
- Diagrama de Sequência (autenticação + abertura de OS)
- RFCs para decisões técnicas
- ADRs para decisões arquiteturais
- Diagrama ER atualizado

---

## IMPLEMENTAR NA ORDEM ABAIXO

---

## REPOSITÓRIO 1: oficina-lambda

### Estrutura
```
oficina-lambda/
├── src/
│   └── handler.py
├── tests/
│   └── test_handler.py
├── template.yaml
├── requirements.txt
├── Makefile
├── README.md
└── .github/
    └── workflows/
        └── deploy.yml
```

### src/handler.py — Lógica completa da Lambda

```python
import json
import os
import re
import boto3
import psycopg2
import jwt
from datetime import datetime, timedelta, timezone

# Configurações via variáveis de ambiente
DB_HOST = os.environ.get("DB_HOST")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ.get("DB_NAME", "oficina")
DB_USER = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
JWT_SECRET = os.environ.get("JWT_SECRET")
JWT_EXPIRATION_HOURS = int(os.environ.get("JWT_EXPIRATION_HOURS", "24"))


def validar_cpf(cpf: str) -> bool:
    """Valida CPF pelo algoritmo dos dois dígitos verificadores."""
    cpf = re.sub(r"[^0-9]", "", cpf)
    if len(cpf) != 11:
        return False
    if len(set(cpf)) == 1:
        return False
    # Primeiro dígito verificador
    soma = sum(int(cpf[i]) * (10 - i) for i in range(9))
    primeiro = 11 - (soma % 11)
    if primeiro >= 10:
        primeiro = 0
    if primeiro != int(cpf[9]):
        return False
    # Segundo dígito verificador
    soma = sum(int(cpf[i]) * (11 - i) for i in range(10))
    segundo = 11 - (soma % 11)
    if segundo >= 10:
        segundo = 0
    if segundo != int(cpf[10]):
        return False
    return True


def buscar_cliente(cpf: str) -> dict | None:
    """Busca cliente no RDS pelo CPF."""
    conn = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
        connect_timeout=5
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, nome, email FROM clientes WHERE documento_numero = %s",
                (cpf,)
            )
            row = cur.fetchone()
            if row:
                return {"id": str(row[0]), "nome": row[1], "email": row[2]}
            return None
    finally:
        conn.close()


def gerar_token(cliente: dict, cpf: str) -> str:
    """Gera JWT com os dados do cliente."""
    payload = {
        "sub": cpf,
        "clienteId": cliente["id"],
        "nome": cliente["nome"],
        "iat": datetime.now(timezone.utc),
        "exp": datetime.now(timezone.utc) + timedelta(hours=JWT_EXPIRATION_HOURS),
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def resposta(status: int, body: dict) -> dict:
    return {
        "statusCode": status,
        "headers": {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
        },
        "body": json.dumps(body),
    }


def lambda_handler(event, context):
    """Entry point da Lambda."""
    try:
        body = json.loads(event.get("body") or "{}")
        cpf_raw = body.get("cpf", "").strip()

        if not cpf_raw:
            return resposta(400, {"erro": "CPF é obrigatório"})

        cpf = re.sub(r"[^0-9]", "", cpf_raw)

        if not validar_cpf(cpf):
            return resposta(400, {"erro": "CPF inválido"})

        cliente = buscar_cliente(cpf)
        if not cliente:
            return resposta(404, {"erro": "Cliente não encontrado"})

        token = gerar_token(cliente, cpf)

        return resposta(200, {
            "token": token,
            "clienteId": cliente["id"],
            "nome": cliente["nome"],
        })

    except psycopg2.Error as e:
        print(f"[ERROR] Banco de dados: {e}")
        return resposta(503, {"erro": "Serviço temporariamente indisponível"})
    except Exception as e:
        print(f"[ERROR] Inesperado: {e}")
        return resposta(500, {"erro": "Erro interno"})
```

### requirements.txt
```
psycopg2-binary==2.9.9
PyJWT==2.8.0
```

### template.yaml — SAM Template para deploy
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Description: Lambda de autenticação por CPF — Oficina Mecânica

Globals:
  Function:
    Timeout: 10
    MemorySize: 256
    Runtime: python3.12
    Environment:
      Variables:
        DB_HOST: !Ref DBHost
        DB_PORT: !Ref DBPort
        DB_NAME: !Ref DBName
        DB_USER: !Ref DBUser
        DB_PASSWORD: !Ref DBPassword
        JWT_SECRET: !Ref JWTSecret
        JWT_EXPIRATION_HOURS: "24"

Parameters:
  DBHost:
    Type: String
  DBPort:
    Type: String
    Default: "5432"
  DBName:
    Type: String
    Default: "oficina"
  DBUser:
    Type: String
  DBPassword:
    Type: String
    NoEcho: true
  JWTSecret:
    Type: String
    NoEcho: true

Resources:
  AuthFunction:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: oficina-autenticacao
      CodeUri: src/
      Handler: handler.lambda_handler
      Description: Autentica cliente por CPF e retorna JWT
      Events:
        AuthEvent:
          Type: Api
          Properties:
            Path: /auth/login
            Method: post

Outputs:
  AuthApiUrl:
    Description: URL do endpoint de autenticação
    Value: !Sub "https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/auth/login"
  AuthFunctionArn:
    Description: ARN da Lambda
    Value: !GetAtt AuthFunction.Arn
```

### tests/test_handler.py
```python
import pytest
from unittest.mock import patch, MagicMock
from src.handler import validar_cpf, lambda_handler


class TestValidarCPF:
    def test_cpf_valido(self):
        assert validar_cpf("529.982.247-25") is True

    def test_cpf_valido_sem_formatacao(self):
        assert validar_cpf("52998224725") is True

    def test_cpf_invalido_digitos_verificadores(self):
        assert validar_cpf("123.456.789-00") is False

    def test_cpf_todos_iguais(self):
        assert validar_cpf("111.111.111-11") is False

    def test_cpf_tamanho_errado(self):
        assert validar_cpf("123") is False

    def test_cpf_vazio(self):
        assert validar_cpf("") is False


class TestLambdaHandler:
    def test_sem_cpf_retorna_400(self):
        event = {"body": "{}"}
        resultado = lambda_handler(event, None)
        assert resultado["statusCode"] == 400

    def test_cpf_invalido_retorna_400(self):
        event = {"body": '{"cpf": "111.111.111-11"}'}
        resultado = lambda_handler(event, None)
        assert resultado["statusCode"] == 400

    @patch("src.handler.buscar_cliente")
    def test_cliente_nao_encontrado_retorna_404(self, mock_buscar):
        mock_buscar.return_value = None
        event = {"body": '{"cpf": "529.982.247-25"}'}
        resultado = lambda_handler(event, None)
        assert resultado["statusCode"] == 404

    @patch("src.handler.gerar_token")
    @patch("src.handler.buscar_cliente")
    def test_autenticacao_bem_sucedida(self, mock_buscar, mock_token):
        mock_buscar.return_value = {
            "id": "uuid-123",
            "nome": "Tony Silva",
            "email": "tony@email.com"
        }
        mock_token.return_value = "eyJ.token.jwt"
        event = {"body": '{"cpf": "529.982.247-25"}'}
        resultado = lambda_handler(event, None)
        assert resultado["statusCode"] == 200
```

### Makefile
```makefile
install:
	pip install -r requirements.txt

test:
	python -m pytest tests/ -v --tb=short

build:
	sam build

local:
	sam local start-api

deploy:
	sam build && sam deploy --guided

deploy-ci:
	sam build && sam deploy \
		--no-confirm-changeset \
		--no-fail-on-empty-changeset \
		--parameter-overrides \
			DBHost=$(DB_HOST) \
			DBPort=$(DB_PORT) \
			DBName=$(DB_NAME) \
			DBUser=$(DB_USER) \
			DBPassword=$(DB_PASSWORD) \
			JWTSecret=$(JWT_SECRET)
```

### .github/workflows/deploy.yml
```yaml
name: Deploy Lambda

on:
  push:
    branches: [main, homolog]
  pull_request:
    branches: [main]

jobs:

  test:
    name: Testes
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - name: Instalar dependências
        run: pip install -r requirements.txt pytest
      - name: Rodar testes
        run: python -m pytest tests/ -v

  deploy-homolog:
    name: Deploy Homologação
    runs-on: ubuntu-latest
    needs: test
    if: github.ref == 'refs/heads/homolog'
    environment: homolog
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: AWS_REGION
      - uses: aws-actions/setup-sam@v2
      - name: Build e Deploy
        run: |
          sam build
          sam deploy \
            --stack-name oficina-lambda-homolog \
            --no-confirm-changeset \
            --no-fail-on-empty-changeset \
            --parameter-overrides \
              DBHost=${{ secrets.DB_HOST }} \
              DBPort=${{ secrets.DB_PORT }} \
              DBName=${{ secrets.DB_NAME }} \
              DBUser=${{ secrets.DB_USER }} \
              DBPassword=${{ secrets.DB_PASSWORD }} \
              JWTSecret=${{ secrets.JWT_SECRET }}

  deploy-prod:
    name: Deploy Produção
    runs-on: ubuntu-latest
    needs: test
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: AWS_REGION
      - uses: aws-actions/setup-sam@v2
      - name: Build e Deploy
        run: |
          sam build
          sam deploy \
            --stack-name oficina-lambda-prod \
            --no-confirm-changeset \
            --no-fail-on-empty-changeset \
            --parameter-overrides \
              DBHost=${{ secrets.DB_HOST }} \
              DBPort=${{ secrets.DB_PORT }} \
              DBName=${{ secrets.DB_NAME }} \
              DBUser=${{ secrets.DB_USER }} \
              DBPassword=${{ secrets.DB_PASSWORD }} \
              JWTSecret=${{ secrets.JWT_SECRET }}
```

### Secrets necessários no GitHub (oficina-lambda)
```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
DB_HOST          (endpoint do RDS — disponível após oficina-infra-db rodar)
DB_PORT          (5432)
DB_NAME          (oficina)
DB_USER          (oficina)
DB_PASSWORD      (senha do RDS)
JWT_SECRET       (mesmo secret usado na aplicação Spring Boot)
```

### README.md do oficina-lambda
```markdown
# oficina-lambda

Function Serverless AWS Lambda para autenticação de clientes por CPF.

## Responsabilidade

1. Valida o CPF pelo algoritmo dos dois dígitos verificadores
2. Consulta o cliente no banco de dados RDS PostgreSQL
3. Gera e retorna um token JWT válido para consumo das APIs protegidas

## Tecnologias

- Python 3.12
- AWS Lambda + API Gateway
- AWS SAM CLI
- psycopg2 (PostgreSQL)
- PyJWT

## Endpoint

POST /auth/login
Body: { "cpf": "529.982.247-25" }
Response: { "token": "eyJ...", "clienteId": "uuid", "nome": "Nome" }

## Execução local

pip install -r requirements.txt
sam build
sam local start-api
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf": "529.982.247-25"}'

## Testes

pip install pytest
python -m pytest tests/ -v

## Deploy manual

sam build && sam deploy --guided

## CI/CD

- Push em homolog → deploy automático em homologação
- Push em main (via PR) → deploy automático em produção
```

---

## REPOSITÓRIO 2: oficina-infra-db

### Estrutura
```
oficina-infra-db/
├── main.tf
├── variables.tf
├── outputs.tf
├── README.md
└── .github/
    └── workflows/
        └── terraform.yml
```

### main.tf
```hcl
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  backend "s3" {
    bucket = "oficina-terraform-state-AWS_ACCOUNT_ID"
    key    = "db/terraform.tfstate"
    region = "AWS_REGION"
  }
}

provider "aws" {
  region = var.region
}

# Busca VPC padrão
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Security Group para o RDS
resource "aws_security_group" "rds" {
  name        = "oficina-rds-sg"
  description = "Permite acesso ao RDS PostgreSQL"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "PostgreSQL"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "oficina-rds-sg"
    Project = "oficina"
  }
}

# Subnet Group para o RDS
resource "aws_db_subnet_group" "oficina" {
  name       = "oficina-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name    = "oficina-db-subnet-group"
    Project = "oficina"
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "oficina" {
  identifier        = "oficina-db"
  engine            = "postgres"
  engine_version    = "16.3"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_name
  username = var.db_user
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.oficina.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible     = true
  skip_final_snapshot     = true
  backup_retention_period = 7
  deletion_protection     = false

  tags = {
    Name    = "oficina-db"
    Project = "oficina"
  }
}
```

### variables.tf
```hcl
variable "region" {
  description = "Região AWS"
  type        = string
  default     = "sa-east-1"
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
  default     = "oficina"
}

variable "db_user" {
  description = "Usuário do banco de dados"
  type        = string
  default     = "oficina"
}

variable "db_password" {
  description = "Senha do banco de dados"
  type        = string
  sensitive   = true
}
```

### outputs.tf
```hcl
output "db_endpoint" {
  description = "Endpoint do RDS"
  value       = aws_db_instance.oficina.endpoint
}

output "db_host" {
  description = "Host do RDS (sem porta)"
  value       = aws_db_instance.oficina.address
}

output "db_port" {
  description = "Porta do RDS"
  value       = aws_db_instance.oficina.port
}

output "db_name" {
  description = "Nome do banco"
  value       = aws_db_instance.oficina.db_name
}
```

### .github/workflows/terraform.yml
```yaml
name: Terraform RDS

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  terraform:
    name: Terraform Plan e Apply
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: AWS_REGION
      - uses: hashicorp/setup-terraform@v3
      - name: Terraform Init
        run: terraform init
      - name: Terraform Plan
        run: terraform plan -var="db_password=${{ secrets.DB_PASSWORD }}"
      - name: Terraform Apply
        if: github.ref == 'refs/heads/main'
        run: terraform apply -auto-approve -var="db_password=${{ secrets.DB_PASSWORD }}"
```

---

## REPOSITÓRIO 3: oficina-infra-k8s

### Estrutura
```
oficina-infra-k8s/
├── main.tf
├── variables.tf
├── outputs.tf
├── k8s/
│   ├── namespace.yaml
│   ├── secret.yaml
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
├── README.md
└── .github/
    └── workflows/
        └── terraform.yml
```

### main.tf
```hcl
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  backend "s3" {
    bucket = "oficina-terraform-state-AWS_ACCOUNT_ID"
    key    = "k8s/terraform.tfstate"
    region = "AWS_REGION"
  }
}

provider "aws" {
  region = var.region
}

# IAM Role para o EKS
resource "aws_iam_role" "eks_cluster" {
  name = "oficina-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster.name
}

# IAM Role para os Node Groups
resource "aws_iam_role" "eks_nodes" {
  name = "oficina-eks-nodes-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_worker_node" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_nodes.name
}

resource "aws_iam_role_policy_attachment" "eks_cni" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_nodes.name
}

resource "aws_iam_role_policy_attachment" "eks_ecr" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_nodes.name
}

# VPC e Subnets padrão
data "aws_vpc" "default" { default = true }
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Cluster EKS
resource "aws_eks_cluster" "oficina" {
  name     = "oficina-cluster"
  role_arn = aws_iam_role.eks_cluster.arn
  version  = "1.30"

  vpc_config {
    subnet_ids = data.aws_subnets.default.ids
  }

  depends_on = [aws_iam_role_policy_attachment.eks_cluster_policy]

  tags = { Name = "oficina-cluster", Project = "oficina" }
}

# Node Group
resource "aws_eks_node_group" "oficina" {
  cluster_name    = aws_eks_cluster.oficina.name
  node_group_name = "oficina-nodes"
  node_role_arn   = aws_iam_role.eks_nodes.arn
  subnet_ids      = data.aws_subnets.default.ids
  instance_types  = ["t3.medium"]

  scaling_config {
    desired_size = 2
    max_size     = 4
    min_size     = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_worker_node,
    aws_iam_role_policy_attachment.eks_cni,
    aws_iam_role_policy_attachment.eks_ecr,
  ]

  tags = { Name = "oficina-nodes", Project = "oficina" }
}
```

### variables.tf
```hcl
variable "region" {
  description = "Região AWS"
  type        = string
  default     = "sa-east-1"
}
```

### outputs.tf
```hcl
output "cluster_name" {
  value = aws_eks_cluster.oficina.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.oficina.endpoint
}

output "kubeconfig_command" {
  description = "Comando para configurar o kubectl"
  value       = "aws eks update-kubeconfig --region AWS_REGION --name oficina-cluster"
}
```

---

## REPOSITÓRIO 4: oficina-app

### Alterações necessárias na aplicação Spring Boot

#### 4.1 Substituir autenticação por CPF

O SecurityConfig deve aceitar o JWT gerado pela Lambda (mesmo secret, mesmo algoritmo HS256).

Remover o endpoint POST /api/auth/login (quem gera o token agora é a Lambda).

Manter o filtro JWT — apenas verificar o token, não mais gerar.

#### 4.2 Adicionar Datadog

Adicionar ao pom.xml:
```xml
<dependency>
    <groupId>com.datadoghq</groupId>
    <artifactId>dd-java-agent</artifactId>
    <version>1.35.0</version>
</dependency>
```

Adicionar ao Dockerfile:
```dockerfile
ADD https://dtdg.co/latest-java-tracer /dd-java-agent.jar
ENTRYPOINT ["java", \
  "-javaagent:/dd-java-agent.jar", \
  "-Ddd.service=oficina-app", \
  "-Ddd.env=${DD_ENV}", \
  "-Ddd.version=${APP_VERSION}", \
  "-Ddd.logs.injection=true", \
  "-Ddd.profiling.enabled=true", \
  "-XX:+UseContainerSupport", \
  "-jar", "app.jar"]
```

Adicionar ao application.properties:
```properties
# Logs estruturados em JSON para Datadog
logging.pattern.console={"timestamp":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%-5level","service":"oficina-app","traceId":"%X{dd.trace_id}","spanId":"%X{dd.span_id}","message":"%msg"}%n
```

#### 4.3 Atualizar CI/CD para EKS

O deploy.yml do oficina-app precisa:
1. Build do JAR
2. Build e push da imagem para ECR (Amazon Elastic Container Registry)
3. Update do kubectl para EKS
4. kubectl apply dos manifestos

```yaml
- name: Login ECR
  uses: aws-actions/amazon-ecr-login@v2

- name: Build e Push imagem
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
  run: |
    docker build -t $ECR_REGISTRY/oficina-app:${{ github.sha }} .
    docker push $ECR_REGISTRY/oficina-app:${{ github.sha }}

- name: Configurar kubectl para EKS
  run: |
    aws eks update-kubeconfig --region AWS_REGION --name oficina-cluster

- name: Deploy no EKS
  run: kubectl apply -f k8s/
```

---

## ANTES DE IMPLEMENTAR — Criar o bucket S3 para estado do Terraform

Os dois repositórios de infra usam backend S3. Criar o bucket antes de rodar terraform init:

```bash
aws s3 mb s3://oficina-terraform-state-AWS_ACCOUNT_ID --region AWS_REGION
aws s3api put-bucket-versioning \
  --bucket oficina-terraform-state-AWS_ACCOUNT_ID \
  --versioning-configuration Status=Enabled
```

---

## ORDEM DE IMPLEMENTAÇÃO

1. 🔲 Criar bucket S3 para estado Terraform
2. 🔲 **oficina-infra-db** — provisionar RDS primeiro (Lambda e app precisam do endpoint)
3. 🔲 **oficina-lambda** — criar função, testar local, fazer deploy
4. 🔲 **oficina-infra-k8s** — provisionar EKS
5. 🔲 **oficina-app** — adaptar autenticação, adicionar Datadog, atualizar CI/CD para EKS
6. 🔲 Configurar API Gateway na AWS Console roteando para Lambda e EKS
7. 🔲 Configurar Datadog (criar conta gratuita, instalar agente no EKS)
8. 🔲 Documentação: ADR, RFC, diagramas
9. 🔲 README em cada repositório
10. 🔲 Vídeo de demonstração
11. 🔲 PDF de entrega com links dos 4 repos + vídeo

---

## SECRETS NECESSÁRIOS POR REPOSITÓRIO

### oficina-lambda e oficina-infra-db e oficina-infra-k8s
```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
DB_PASSWORD
JWT_SECRET
DB_HOST         (após RDS criado)
```

### oficina-app
```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
ECR_REGISTRY
JWT_SECRET
DD_API_KEY      (após criar conta Datadog)
```

---

## CUSTOS ESTIMADOS (AWS Free Tier)

| Serviço | Free Tier | Estimativa mensal |
|---|---|---|
| Lambda | 1M requests/mês grátis | $0 |
| API Gateway | 1M requests/mês grátis | $0 |
| RDS t3.micro | 750h/mês grátis | $0 |
| EKS cluster | $0.10/h (não tem free) | ~$7/mês |
| EC2 t3.medium (2 nós) | 750h t2/t3.micro | ~$60/mês |

**Atenção:** EKS não é gratuito. Para minimizar custo, use `t3.small` nos nós e destrua o cluster após gravar o vídeo com `terraform destroy`.

---

## REFERÊNCIAS

- AWS SAM: https://docs.aws.amazon.com/serverless-application-model/
- AWS EKS: https://docs.aws.amazon.com/eks/
- AWS RDS: https://docs.aws.amazon.com/rds/
- Datadog K8s: https://docs.datadoghq.com/containers/kubernetes/
- Terraform AWS: https://registry.terraform.io/providers/hashicorp/aws