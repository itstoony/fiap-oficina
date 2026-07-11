package br.com.fiap.oficina.atendimento.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AtendenteJpaRepository extends JpaRepository<Atendente, UUID> {
    boolean existsByEmail(String email);
}
