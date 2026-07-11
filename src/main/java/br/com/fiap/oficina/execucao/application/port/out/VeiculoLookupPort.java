package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Veiculo;

import java.util.UUID;

public interface VeiculoLookupPort {

    Veiculo buscarPorId(UUID id);
}
