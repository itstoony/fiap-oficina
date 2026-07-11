package br.com.fiap.oficina.administracao.adapter.out.persistence;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicoJpaRepository extends JpaRepository<Servico, UUID> {
    boolean existsByNome(String nome);
}
