package br.com.fiap.oficina.atendimento.adapter.out.persistence;

import br.com.fiap.oficina.atendimento.application.port.out.AtendenteRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AtendenteRepositoryAdapter implements AtendenteRepositoryPort {

    private final AtendenteJpaRepository jpaRepository;

    @Override
    public Atendente salvar(Atendente atendente) {
        return jpaRepository.save(atendente);
    }

    @Override
    public Optional<Atendente> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<Atendente> listarTodos() {
        return jpaRepository.findAll();
    }

    @Override
    public void deletar(Atendente atendente) {
        jpaRepository.delete(atendente);
    }
}
