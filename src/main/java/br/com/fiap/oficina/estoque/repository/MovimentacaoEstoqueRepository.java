package br.com.fiap.oficina.estoque.repository;

import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {
    List<MovimentacaoEstoque> findByPecaIdOrderByDataMovimentacaoDesc(UUID pecaId);
}
