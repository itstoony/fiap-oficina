package br.com.fiap.oficina.estoque.adapter.out.persistence;

import br.com.fiap.oficina.estoque.application.port.out.PecaRepositoryPort;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PecaRepositoryAdapter implements PecaRepositoryPort {

    private final PecaJpaRepository jpaRepository;

    @Override
    public Peca salvar(Peca peca) {
        return jpaRepository.save(peca);
    }

    @Override
    public Optional<Peca> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return jpaRepository.existsByCodigo(codigo);
    }

    @Override
    public List<Peca> listarTodos() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Peca> buscarCriticas() {
        return jpaRepository.findCriticas();
    }

    @Override
    public void deletar(Peca peca) {
        jpaRepository.delete(peca);
    }
}
