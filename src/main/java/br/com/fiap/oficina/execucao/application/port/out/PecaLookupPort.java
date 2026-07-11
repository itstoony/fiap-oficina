package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.estoque.domain.model.Peca;

import java.util.UUID;

public interface PecaLookupPort {

    Peca buscarPorId(UUID id);
}
