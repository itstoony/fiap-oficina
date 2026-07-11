resource "kubernetes_namespace" "oficina" {
  metadata {
    name = "oficina"
  }

  depends_on = [kind_cluster.oficina]
}

resource "kubernetes_secret" "oficina_secrets" {
  metadata {
    name      = "oficina-secrets"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  type = "Opaque"

  data = {
    "db-password"   = var.db_password
    "jwt-secret"    = var.jwt_secret
    "mail-username" = var.mail_username
    "mail-password" = var.mail_password
  }

  depends_on = [kubernetes_namespace.oficina]
}

resource "kubernetes_config_map" "oficina_config" {
  metadata {
    name      = "oficina-config"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://oficina-db:5432/oficina"
    SPRING_DATASOURCE_USERNAME = "oficina"
    SPRING_PROFILES_ACTIVE     = "prod"
    MAIL_HOST                  = "smtp.gmail.com"
    MAIL_PORT                  = "587"
    APP_URL                    = "http://oficina-app:8080"
  }

  depends_on = [kubernetes_namespace.oficina]
}
