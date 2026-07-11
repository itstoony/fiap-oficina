package br.com.fiap.oficina.atendimento.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AtendenteRepositoryPort {
    Atendente salvar(Atendente atendente);
    Optional<Atendente> buscarPorId(UUID id);
    boolean existePorEmail(String email);
    List<Atendente> listarTodos();
    void deletar(Atendente atendente);
}
