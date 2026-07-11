package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.adapter.out.persistence.VeiculoJpaRepository;
import br.com.fiap.oficina.execucao.application.port.out.VeiculoLookupPort;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeiculoLookupAdapter implements VeiculoLookupPort {

    private final VeiculoJpaRepository veiculoRepository;

    @Override
    public Veiculo buscarPorId(UUID id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado com id: " + id));
    }
}
