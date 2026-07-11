package br.com.fiap.oficina.estoque.application.port.out;

import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepositoryPort {
    MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao);
    List<MovimentacaoEstoque> buscarPorPecaIdOrdenado(UUID pecaId);
}
