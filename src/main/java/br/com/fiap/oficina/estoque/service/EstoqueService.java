package br.com.fiap.oficina.estoque.service;

import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.repository.PecaRepository;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stub do EstoqueService — reserva e disponibilidade implementados com lógica básica.
 * Baixa definitiva e liberação de reservas serão implementadas no bounded context de Estoque.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final PecaRepository pecaRepository;

    @Transactional
    public void verificarDisponibilidadeEReservar(UUID pecaId, Integer quantidade, UUID osId) {
        Peca peca = pecaRepository.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));

        if (peca.getQtdDisponivel() < quantidade) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente para a peça '" + peca.getNome() +
                    "'. Disponível: " + peca.getQtdDisponivel() + ", solicitado: " + quantidade);
        }

        peca.setQtdReservada(peca.getQtdReservada() + quantidade);
        pecaRepository.save(peca);
        log.info("Reserva criada: {} unidades de '{}' para OS {}", quantidade, peca.getNome(), osId);
    }

    @Transactional
    public void liberarReserva(UUID pecaId, Integer quantidade, UUID osId) {
        pecaRepository.findById(pecaId).ifPresent(peca -> {
            int novaReserva = Math.max(0, peca.getQtdReservada() - quantidade);
            peca.setQtdReservada(novaReserva);
            pecaRepository.save(peca);
            log.info("Reserva liberada: {} unidades de '{}' da OS {}", quantidade, peca.getNome(), osId);
        });
    }

    @Transactional
    public void baixarEstoque(UUID osId, UUID pecaId, Integer quantidade) {
        // TODO: implementar baixa definitiva com registro em MovimentacaoEstoque ao implementar o bounded context de Estoque
        Peca peca = pecaRepository.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));

        peca.setQtdEstoque(peca.getQtdEstoque() - quantidade);
        peca.setQtdReservada(Math.max(0, peca.getQtdReservada() - quantidade));
        pecaRepository.save(peca);
        log.info("Baixa de estoque: {} unidades de '{}' para OS {}", quantidade, peca.getNome(), osId);
    }
}
