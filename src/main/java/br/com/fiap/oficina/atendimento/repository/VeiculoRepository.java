package br.com.fiap.oficina.atendimento.repository;

import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    Optional<Veiculo> findByPlacaValor(String valor);
    boolean existsByPlacaValor(String valor);
    List<Veiculo> findByClienteId(UUID clienteId);
}