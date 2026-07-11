package br.com.fiap.oficina.atendimento.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Veiculo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepositoryPort {
    Veiculo salvar(Veiculo veiculo);
    Optional<Veiculo> buscarPorId(UUID id);
    Optional<Veiculo> buscarPorPlacaValor(String valor);
    boolean existePorPlacaValor(String valor);
    List<Veiculo> buscarPorClienteId(UUID clienteId);
    List<Veiculo> listarTodos();
    void deletar(Veiculo veiculo);
}
