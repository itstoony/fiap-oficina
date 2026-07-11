package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.adapter.out.persistence.PecaJpaRepository;
import br.com.fiap.oficina.execucao.application.port.out.PecaLookupPort;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PecaLookupAdapter implements PecaLookupPort {

    private final PecaJpaRepository pecaRepository;

    @Override
    public Peca buscarPorId(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Peça não encontrada com id: " + id));
    }
}
