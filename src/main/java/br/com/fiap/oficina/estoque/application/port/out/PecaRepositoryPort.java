package br.com.fiap.oficina.estoque.application.port.out;

import br.com.fiap.oficina.estoque.domain.model.Peca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepositoryPort {
    Peca salvar(Peca peca);
    Optional<Peca> buscarPorId(UUID id);
    boolean existePorCodigo(String codigo);
    List<Peca> listarTodos();
    List<Peca> buscarCriticas();
    void deletar(Peca peca);
}
