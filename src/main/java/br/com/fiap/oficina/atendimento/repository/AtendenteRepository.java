package br.com.fiap.oficina.atendimento.repository;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AtendenteRepository extends JpaRepository<Atendente, UUID> {
    boolean existsByEmail(String email);
}