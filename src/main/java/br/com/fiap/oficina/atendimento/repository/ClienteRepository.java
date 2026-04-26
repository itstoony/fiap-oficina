package br.com.fiap.oficina.atendimento.repository;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByDocumentoNumero(String numero);
    boolean existsByDocumentoNumero(String numero);
}