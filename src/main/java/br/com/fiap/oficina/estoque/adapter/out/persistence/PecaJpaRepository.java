package br.com.fiap.oficina.estoque.adapter.out.persistence;

import br.com.fiap.oficina.estoque.domain.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PecaJpaRepository extends JpaRepository<Peca, UUID> {
    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM Peca p WHERE p.qtdEstoque <= p.qtdMinima")
    List<Peca> findCriticas();
}
