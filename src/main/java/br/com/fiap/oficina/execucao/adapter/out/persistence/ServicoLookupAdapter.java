package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.administracao.adapter.out.persistence.ServicoJpaRepository;
import br.com.fiap.oficina.execucao.application.port.out.ServicoLookupPort;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServicoLookupAdapter implements ServicoLookupPort {

    private final ServicoJpaRepository servicoRepository;

    @Override
    public Servico buscarPorId(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Serviço não encontrado com id: " + id));
    }
}
