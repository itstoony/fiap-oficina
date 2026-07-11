output "cluster_name" {
  description = "Nome do cluster Kind criado"
  value       = kind_cluster.oficina.name
}

output "cluster_endpoint" {
  description = "Endpoint do cluster Kubernetes"
  value       = kind_cluster.oficina.endpoint
}

output "kubeconfig" {
  description = "Kubeconfig para acesso ao cluster"
  value       = kind_cluster.oficina.kubeconfig
  sensitive   = true
}
