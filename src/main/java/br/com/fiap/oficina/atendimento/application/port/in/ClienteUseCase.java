package br.com.fiap.oficina.atendimento.application.port.in;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;

import java.util.List;
import java.util.UUID;

public interface ClienteUseCase {
    ClienteDTO.Response cadastrar(ClienteDTO.CadastrarRequest request);
    List<ClienteDTO.Response> listar();
    ClienteDTO.Response buscarPorId(UUID id);
    ClienteDTO.Response buscarPorDocumento(String documento);
    ClienteDTO.Response atualizar(UUID id, ClienteDTO.AtualizarRequest request);
    void excluir(UUID id);
    Cliente buscarEntidade(UUID id);
}
