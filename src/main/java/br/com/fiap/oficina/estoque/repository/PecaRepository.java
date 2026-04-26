package br.com.fiap.oficina.estoque.repository;

import br.com.fiap.oficina.estoque.domain.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {
    boolean existsByCodigo(String codigo);
}
