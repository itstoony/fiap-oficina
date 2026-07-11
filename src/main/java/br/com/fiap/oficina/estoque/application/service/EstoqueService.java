package br.com.fiap.oficina.estoque.application.service;

import br.com.fiap.oficina.estoque.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.fiap.oficina.estoque.application.port.out.PecaRepositoryPort;
import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final PecaRepositoryPort pecaRepository;
    private final MovimentacaoEstoqueRepositoryPort movimentacaoRepository;

    @Transactional
    public void verificarDisponibilidadeEReservar(UUID pecaId, Integer quantidade, UUID osId) {
        Peca peca = buscarPeca(pecaId);

        if (peca.getQtdDisponivel() < quantidade) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente para a peça '" + peca.getNome() +
                    "'. Disponível: " + peca.getQtdDisponivel() + ", solicitado: " + quantidade);
        }

        peca.setQtdReservada(peca.getQtdReservada() + quantidade);
        pecaRepository.salvar(peca);

        registrar(peca, TipoMovimentacao.RESERVA, quantidade, osId, "Reserva para OS " + osId);
        log.info("Reserva criada: {} unidades de '{}' para OS {}", quantidade, peca.getNome(), osId);
    }

    @Transactional
    public void liberarReserva(UUID pecaId, Integer quantidade, UUID osId) {
        pecaRepository.buscarPorId(pecaId).ifPresent(peca -> {
            int novaReserva = Math.max(0, peca.getQtdReservada() - quantidade);
            peca.setQtdReservada(novaReserva);
            pecaRepository.salvar(peca);

            registrar(peca, TipoMovimentacao.LIBERACAO_RESERVA, quantidade, osId, "Liberação de reserva da OS " + osId);
            log.info("Reserva liberada: {} unidades de '{}' da OS {}", quantidade, peca.getNome(), osId);
        });
    }

    @Transactional
    public void baixarEstoque(UUID osId, UUID pecaId, Integer quantidade) {
        Peca peca = buscarPeca(pecaId);

        peca.setQtdEstoque(peca.getQtdEstoque() - quantidade);
        peca.setQtdReservada(Math.max(0, peca.getQtdReservada() - quantidade));
        pecaRepository.salvar(peca);

        registrar(peca, TipoMovimentacao.BAIXA, quantidade, osId, "Baixa definitiva para OS " + osId);
        log.info("Baixa de estoque: {} unidades de '{}' para OS {}", quantidade, peca.getNome(), osId);
    }

    private Peca buscarPeca(UUID pecaId) {
        return pecaRepository.buscarPorId(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
    }

    private void registrar(Peca peca, TipoMovimentacao tipo, Integer quantidade, UUID osId, String observacao) {
        movimentacaoRepository.salvar(MovimentacaoEstoque.builder()
                .peca(peca)
                .tipo(tipo)
                .quantidade(quantidade)
                .osId(osId)
                .observacao(observacao)
                .build());
    }
}
