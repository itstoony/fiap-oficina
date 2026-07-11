package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.estoque.application.service.EstoqueService;
import br.com.fiap.oficina.execucao.application.port.out.EstoquePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EstoqueAdapter implements EstoquePort {

    private final EstoqueService estoqueService;

    @Override
    public void verificarDisponibilidadeEReservar(UUID pecaId, Integer quantidade, UUID osId) {
        estoqueService.verificarDisponibilidadeEReservar(pecaId, quantidade, osId);
    }

    @Override
    public void liberarReserva(UUID pecaId, Integer quantidade, UUID osId) {
        estoqueService.liberarReserva(pecaId, quantidade, osId);
    }

    @Override
    public void baixarEstoque(UUID osId, UUID pecaId, Integer quantidade) {
        estoqueService.baixarEstoque(osId, pecaId, quantidade);
    }
}
