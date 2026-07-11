package br.com.fiap.oficina.estoque.adapter.out.persistence;

import br.com.fiap.oficina.estoque.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovimentacaoEstoqueRepositoryAdapter implements MovimentacaoEstoqueRepositoryPort {

    private final MovimentacaoEstoqueJpaRepository jpaRepository;

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao) {
        return jpaRepository.save(movimentacao);
    }

    @Override
    public List<MovimentacaoEstoque> buscarPorPecaIdOrdenado(UUID pecaId) {
        return jpaRepository.findByPecaIdOrderByDataMovimentacaoDesc(pecaId);
    }
}
