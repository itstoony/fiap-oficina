package br.com.fiap.oficina.administracao.application.port.in;

import br.com.fiap.oficina.administracao.domain.model.Servico;

import java.util.List;
import java.util.UUID;

public interface ServicoUseCase {
    ServicoDTO.Response cadastrar(ServicoDTO.CadastrarRequest request);
    List<ServicoDTO.Response> listar();
    ServicoDTO.Response buscarPorId(UUID id);
    Servico buscarEntidade(UUID id);
    ServicoDTO.Response atualizar(UUID id, ServicoDTO.AtualizarRequest request);
    void excluir(UUID id);
}
