# Infraestrutura — Terraform + Kind

Scripts Terraform para provisionamento de um cluster Kubernetes local com [Kind](https://kind.sigs.k8s.io/).

## Pré-requisitos

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.5.0
- [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Docker](https://docs.docker.com/get-docker/) (em execução)

## Estrutura

```
infra/
├── main.tf        # Providers e cluster Kind (1 control-plane + 2 workers)
├── variables.tf   # Variáveis configuráveis (cluster_name, db_password, jwt_secret, etc.)
├── outputs.tf     # Outputs: cluster_name, cluster_endpoint, kubeconfig
└── kubernetes.tf  # Recursos Kubernetes: namespace, secret, configmap
```

## Uso

### 1. Inicializar o Terraform

```bash
cd infra/
terraform init
```

### 2. (Opcional) Personalizar variáveis

Crie um arquivo `terraform.tfvars`:

```hcl
cluster_name  = "oficina-cluster"
db_password   = "senha-segura"
jwt_secret    = "meu-jwt-secret-minimo-256-bits"
mail_username = "usuario@gmail.com"
mail_password = "senha-app"
```

### 3. Planejar e aplicar

```bash
terraform plan
terraform apply
```

### 4. Configurar kubectl

```bash
terraform output -raw kubeconfig > ~/.kube/config-oficina
export KUBECONFIG=~/.kube/config-oficina
kubectl get nodes
```

### 5. Aplicar manifests Kubernetes

```bash
kubectl apply -f ../k8s/
```

### 6. Destruir o ambiente

```bash
terraform destroy
```

## Recursos criados

| Recurso | Tipo | Descrição |
|---------|------|-----------|
| `oficina-cluster` | Kind Cluster | 1 control-plane + 2 workers |
| `oficina` | Namespace | Namespace da aplicação |
| `oficina-secrets` | Secret | Credenciais (db, jwt, mail) |
| `oficina-config` | ConfigMap | Configurações da aplicação |
