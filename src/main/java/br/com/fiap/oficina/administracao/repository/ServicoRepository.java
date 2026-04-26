package br.com.fiap.oficina.administracao.repository;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
}
