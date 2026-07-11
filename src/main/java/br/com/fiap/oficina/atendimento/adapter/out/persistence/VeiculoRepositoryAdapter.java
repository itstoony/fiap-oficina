package br.com.fiap.oficina.atendimento.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.application.port.out.VeiculoRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeiculoRepositoryAdapter implements VeiculoRepositoryPort {

    private final VeiculoJpaRepository jpaRepository;

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        return jpaRepository.save(veiculo);
    }

    @Override
    public Optional<Veiculo> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Veiculo> buscarPorPlacaValor(String valor) {
        return jpaRepository.findByPlacaValor(valor);
    }

    @Override
    public boolean existePorPlacaValor(String valor) {
        return jpaRepository.existsByPlacaValor(valor);
    }

    @Override
    public List<Veiculo> buscarPorClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId);
    }

    @Override
    public List<Veiculo> listarTodos() {
        return jpaRepository.findAll();
    }

    @Override
    public void deletar(Veiculo veiculo) {
        jpaRepository.delete(veiculo);
    }
}
