package br.com.fiap.oficina.execucao.application.port.in;

import java.util.List;
import java.util.UUID;

public interface OrdemDeServicoAdminUseCase {

    OrdemDeServicoDTO.Response criar(OrdemDeServicoDTO.CriarRequest request);

    List<OrdemDeServicoDTO.Response> listar();

    OrdemDeServicoDTO.Response buscarPorId(UUID id);

    OrdemDeServicoDTO.Response adicionarServico(UUID osId, ItemServicoDTO.AdicionarRequest request);

    OrdemDeServicoDTO.Response removerServico(UUID osId, UUID itemId);

    OrdemDeServicoDTO.Response adicionarPeca(UUID osId, ItemPecaDTO.AdicionarRequest request);

    OrdemDeServicoDTO.Response removerPeca(UUID osId, UUID itemId);

    OrdemDeServicoDTO.Response iniciarDiagnostico(UUID id);

    OrdemDeServicoDTO.Response enviarOrcamento(UUID id);

    OrdemDeServicoDTO.Response iniciarExecucao(UUID id, OrdemDeServicoDTO.IniciarExecucaoRequest request);

    OrdemDeServicoDTO.Response finalizar(UUID id);

    OrdemDeServicoDTO.Response entregar(UUID id);

    OrdemDeServicoDTO.Response cancelar(UUID id);
}
