package br.com.fiap.oficina.administracao.application.port.out;

import br.com.fiap.oficina.administracao.domain.model.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepositoryPort {
    Servico salvar(Servico servico);
    Optional<Servico> buscarPorId(UUID id);
    boolean existePorNome(String nome);
    List<Servico> listarTodos();
    void deletar(Servico servico);
}
