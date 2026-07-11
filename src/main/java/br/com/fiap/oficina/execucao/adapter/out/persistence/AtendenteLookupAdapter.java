package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.adapter.out.persistence.AtendenteJpaRepository;
import br.com.fiap.oficina.execucao.application.port.out.AtendenteLookupPort;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AtendenteLookupAdapter implements AtendenteLookupPort {

    private final AtendenteJpaRepository atendenteRepository;

    @Override
    public Atendente buscarPorId(UUID id) {
        return atendenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendente não encontrado com id: " + id));
    }
}
