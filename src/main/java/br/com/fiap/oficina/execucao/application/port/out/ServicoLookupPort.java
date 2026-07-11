package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.administracao.domain.model.Servico;

import java.util.UUID;

public interface ServicoLookupPort {

    Servico buscarPorId(UUID id);
}
