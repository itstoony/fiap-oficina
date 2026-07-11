package br.com.fiap.oficina.atendimento.application.port.in;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;

import java.util.List;
import java.util.UUID;

public interface AtendenteUseCase {
    AtendenteDTO.Response cadastrar(AtendenteDTO.CadastrarRequest request);
    List<AtendenteDTO.Response> listar();
    AtendenteDTO.Response buscarPorId(UUID id);
    AtendenteDTO.Response atualizar(UUID id, AtendenteDTO.AtualizarRequest request);
    void excluir(UUID id);
    Atendente buscarEntidade(UUID id);
}
