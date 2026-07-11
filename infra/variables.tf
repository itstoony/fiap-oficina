variable "cluster_name" {
  description = "Nome do cluster Kind"
  type        = string
  default     = "oficina-cluster"
}

variable "db_password" {
  description = "Senha do banco de dados PostgreSQL"
  type        = string
  sensitive   = true
  default     = "oficina"
}

variable "jwt_secret" {
  description = "Segredo JWT para assinatura dos tokens"
  type        = string
  sensitive   = true
  default     = "chave-secreta-minimo-256-bits-para-o-jwt-do-sistema-oficina"
}

variable "mail_username" {
  description = "Usuário do servidor de e-mail"
  type        = string
  sensitive   = true
  default     = ""
}

variable "mail_password" {
  description = "Senha do servidor de e-mail"
  type        = string
  sensitive   = true
  default     = ""
}
