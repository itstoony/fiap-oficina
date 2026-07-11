package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;

import java.util.List;
import java.util.UUID;

public interface OrdemDeServicoRepositoryPort {

    OrdemDeServico salvar(OrdemDeServico ordemDeServico);

    OrdemDeServico buscarPorId(UUID id);

    OrdemDeServico buscarPorNumero(String numero);

    List<OrdemDeServico> listarAtivasOrdenadas();

    List<OrdemDeServico> listarTodas();

    int buscarProximoSequencial(String prefixo);

    List<OrdemDeServico> buscarFinalizadasComDatas(List<StatusOS> statuses);
}
