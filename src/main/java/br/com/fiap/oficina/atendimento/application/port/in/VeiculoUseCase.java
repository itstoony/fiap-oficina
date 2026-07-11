package br.com.fiap.oficina.atendimento.application.port.in;

import java.util.List;
import java.util.UUID;

public interface VeiculoUseCase {
    VeiculoDTO.Response cadastrar(VeiculoDTO.CadastrarRequest request);
    List<VeiculoDTO.Response> listar();
    VeiculoDTO.Response buscarPorId(UUID id);
    VeiculoDTO.Response buscarPorPlaca(String placa);
    List<VeiculoDTO.Response> buscarPorCliente(UUID clienteId);
    VeiculoDTO.Response atualizar(UUID id, VeiculoDTO.AtualizarRequest request);
    void excluir(UUID id);
}
