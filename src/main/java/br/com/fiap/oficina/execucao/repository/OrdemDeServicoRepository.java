package br.com.fiap.oficina.execucao.repository;

import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, UUID> {

    Optional<OrdemDeServico> findByNumero(String numero);

    @Query("SELECT os FROM OrdemDeServico os " +
           "LEFT JOIN FETCH os.itensServico is LEFT JOIN FETCH is.servico " +
           "LEFT JOIN FETCH os.itensPeca ip LEFT JOIN FETCH ip.peca " +
           "WHERE os.id = :id")
    Optional<OrdemDeServico> findByIdComItens(@Param("id") UUID id);

    @Query("SELECT os FROM OrdemDeServico os " +
           "LEFT JOIN FETCH os.itensServico is LEFT JOIN FETCH is.servico " +
           "LEFT JOIN FETCH os.itensPeca ip LEFT JOIN FETCH ip.peca " +
           "WHERE os.numero = :numero")
    Optional<OrdemDeServico> findByNumeroComItens(@Param("numero") String numero);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero, 9, 5) AS INTEGER)), 0) " +
                   "FROM ordens_de_servico WHERE numero LIKE ?1",
           nativeQuery = true)
    Integer findMaxSequencialByPrefixo(String prefixo);

    @Query("SELECT os FROM OrdemDeServico os WHERE os.status IN :statuses " +
           "AND os.dataInicioExecucao IS NOT NULL AND os.dataFimExecucao IS NOT NULL")
    List<OrdemDeServico> findFinalizadasComDatas(@Param("statuses") List<StatusOS> statuses);
}
