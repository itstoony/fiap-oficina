package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;

import java.util.UUID;

public interface ClienteLookupPort {

    Cliente buscarPorId(UUID id);
}
