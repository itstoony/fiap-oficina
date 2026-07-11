package br.com.fiap.oficina.execucao.application.port.out;

import java.util.UUID;

public interface EstoquePort {

    void verificarDisponibilidadeEReservar(UUID pecaId, Integer quantidade, UUID osId);

    void liberarReserva(UUID pecaId, Integer quantidade, UUID osId);

    void baixarEstoque(UUID osId, UUID pecaId, Integer quantidade);
}
