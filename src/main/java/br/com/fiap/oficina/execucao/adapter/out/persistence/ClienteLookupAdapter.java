package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.adapter.out.persistence.ClienteJpaRepository;
import br.com.fiap.oficina.execucao.application.port.out.ClienteLookupPort;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClienteLookupAdapter implements ClienteLookupPort {

    private final ClienteJpaRepository clienteRepository;

    @Override
    public Cliente buscarPorId(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado com id: " + id));
    }
}
