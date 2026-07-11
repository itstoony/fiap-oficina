package br.com.fiap.oficina.atendimento.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.application.port.out.ClienteRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository jpaRepository;

    @Override
    public Cliente salvar(Cliente cliente) {
        return jpaRepository.save(cliente);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Cliente> buscarPorDocumentoNumero(String numero) {
        return jpaRepository.findByDocumentoNumero(numero);
    }

    @Override
    public boolean existePorDocumentoNumero(String numero) {
        return jpaRepository.existsByDocumentoNumero(numero);
    }

    @Override
    public List<Cliente> listarTodos() {
        return jpaRepository.findAll();
    }

    @Override
    public void deletar(Cliente cliente) {
        jpaRepository.delete(cliente);
    }
}
