package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;

import java.util.UUID;

public interface AtendenteLookupPort {

    Atendente buscarPorId(UUID id);
}
