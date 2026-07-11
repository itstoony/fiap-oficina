package br.com.fiap.oficina.atendimento.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {
    Cliente salvar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    Optional<Cliente> buscarPorDocumentoNumero(String numero);
    boolean existePorDocumentoNumero(String numero);
    List<Cliente> listarTodos();
    void deletar(Cliente cliente);
}
