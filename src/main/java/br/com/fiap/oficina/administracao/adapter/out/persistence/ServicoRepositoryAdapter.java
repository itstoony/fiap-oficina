package br.com.fiap.oficina.administracao.adapter.out.persistence;

import br.com.fiap.oficina.administracao.application.port.out.ServicoRepositoryPort;
import br.com.fiap.oficina.administracao.domain.model.Servico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServicoRepositoryAdapter implements ServicoRepositoryPort {

    private final ServicoJpaRepository jpaRepository;

    @Override
    public Servico salvar(Servico servico) {
        return jpaRepository.save(servico);
    }

    @Override
    public Optional<Servico> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existePorNome(String nome) {
        return jpaRepository.existsByNome(nome);
    }

    @Override
    public List<Servico> listarTodos() {
        return jpaRepository.findAll();
    }

    @Override
    public void deletar(Servico servico) {
        jpaRepository.delete(servico);
    }
}
