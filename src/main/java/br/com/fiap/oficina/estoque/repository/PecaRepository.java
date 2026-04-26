package br.com.fiap.oficina.estoque.repository;

import br.com.fiap.oficina.estoque.domain.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {
    boolean existsByCodigo(String codigo);
    Optional<Peca> findByCodigo(String codigo);

    @Query("SELECT p FROM Peca p WHERE p.qtdEstoque <= p.qtdMinima")
    List<Peca> findCriticas();
}
